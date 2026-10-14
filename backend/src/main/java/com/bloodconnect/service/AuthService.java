
package com.bloodconnect.service;

import com.bloodconnect.model.*;
import com.bloodconnect.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DonorRepository donorRepository;

    @Autowired
    private HospitalRepository hospitalRepository;

    public static final String ADMIN_EMAIL = "admin@bloodconnect.com";
    public static final String ADMIN_PASSWORD = "Admin@123";

    @Autowired
    private BloodInventoryRepository inventoryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public Map<String, Object> login(String email, String password) {
        return login(email, password, null);
    }

    public Map<String, Object> login(String email, String password, String selectedRole) {
        if (email == null || password == null) {
            throw new RuntimeException("Email and password are required");
        }

        // 1. Primary admin authentication (checks database first if password was reset, otherwise uses default credentials)
        if (ADMIN_EMAIL.equalsIgnoreCase(email.trim())) {
            Optional<User> adminOpt = userRepository.findByEmail(ADMIN_EMAIL);
            boolean matched = false;
            Long adminId = 0L;
            String fullName = "System Administrator";
            String phone = "9876543210";
            if (adminOpt.isPresent()) {
                adminId = adminOpt.get().getId();
                fullName = adminOpt.get().getFullName();
                phone = adminOpt.get().getPhone();
                if (passwordEncoder.matches(password, adminOpt.get().getPasswordHash())) {
                    matched = true;
                }
            }
            if (!matched && ADMIN_PASSWORD.equals(password)) {
                matched = true;
            }

            if (matched) {
                if (selectedRole != null && !selectedRole.trim().isEmpty()
                        && !"ADMIN".equalsIgnoreCase(selectedRole.trim())) {
                    throw new RuntimeException("Role mismatch: Specified role does not match account privileges");
                }
                Map<String, Object> adminResult = new HashMap<>();
                adminResult.put("id", adminId);
                adminResult.put("email", ADMIN_EMAIL);
                adminResult.put("fullName", fullName);
                adminResult.put("phone", phone);
                adminResult.put("role", "ADMIN");
                return adminResult;
            } else {
                throw new RuntimeException("Invalid email or password");
            }
        }

        // 2. Database authentication for database users (CUSTOMER, HOSPITAL, or
        // database-created ADMIN)
        User user = userRepository.findByEmail(email.trim())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new RuntimeException("Invalid email or password");
        }

        if (selectedRole != null && !selectedRole.trim().isEmpty()
                && !user.getRole().equalsIgnoreCase(selectedRole.trim())) {
            throw new RuntimeException("Role mismatch: Specified role does not match account privileges");
        }

        if (!user.getIsActive()) {
            if ("HOSPITAL".equals(user.getRole())) {
                throw new RuntimeException("Hospital account is pending administrator approval");
            }
            throw new RuntimeException("Account has been deactivated. Please contact admin.");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("id", user.getId());
        result.put("email", user.getEmail());
        result.put("fullName", user.getFullName());
        result.put("phone", user.getPhone());
        result.put("role", user.getRole());

        if ("CUSTOMER".equals(user.getRole())) {
            donorRepository.findByUserId(user.getId()).ifPresent(d -> result.put("donorId", d.getId()));
        } else if ("HOSPITAL".equals(user.getRole())) {
            hospitalRepository.findByUserId(user.getId()).ifPresent(h -> {
                result.put("hospitalId", h.getId());
                result.put("hospitalName", h.getHospitalName());
            });
        }

        return result;
    }

    @Transactional
    public User createAdmin(Map<String, Object> req, String requesterEmail) {
        if (requesterEmail == null || requesterEmail.trim().isEmpty()) {
            throw new RuntimeException("Unauthorized: Administrator authentication required");
        }

        // Verify requester is either the primary hardcoded admin or an active database
        // admin
        boolean isAuthorized = false;
        if (ADMIN_EMAIL.equalsIgnoreCase(requesterEmail.trim())) {
            isAuthorized = true;
        } else {
            Optional<User> requesterOpt = userRepository.findByEmail(requesterEmail.trim());
            if (requesterOpt.isPresent() && requesterOpt.get().getIsActive()
                    && "ADMIN".equals(requesterOpt.get().getRole())) {
                isAuthorized = true;
            }
        }

        if (!isAuthorized) {
            throw new RuntimeException("Unauthorized: Only administrators can create admin accounts");
        }

        String fullName = req.get("fullName") != null ? ((String) req.get("fullName")).trim() : "";
        String email = req.get("email") != null ? ((String) req.get("email")).trim() : "";
        String password = req.get("password") != null ? (String) req.get("password") : "";

        if (fullName.isEmpty()) {
            throw new RuntimeException("Full name is required");
        }
        if (email.isEmpty()) {
            throw new RuntimeException("Email is required");
        }
        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            throw new RuntimeException("Invalid email format");
        }
        if (password.isEmpty()) {
            throw new RuntimeException("Password is required");
        }
        if (req.containsKey("confirmPassword")) {
            String confirmPassword = (String) req.get("confirmPassword");
            if (!password.equals(confirmPassword)) {
                throw new RuntimeException("Passwords do not match");
            }
        }

        // Email uniqueness validation against primary admin and existing database users
        if (ADMIN_EMAIL.equalsIgnoreCase(email) || userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email is already registered");
        }

        String phone = req.get("phone") != null && !((String) req.get("phone")).trim().isEmpty()
                ? ((String) req.get("phone")).trim()
                : "N/A";

        User newAdmin = new User(
                email,
                passwordEncoder.encode(password),
                fullName,
                phone,
                "ADMIN",
                true);

        return userRepository.save(newAdmin);
    }

    @Transactional
    public User registerCustomer(Map<String, Object> req) {
        String email = (String) req.get("email");
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email is already registered");
        }

        String password = (String) req.get("password");
        String fullName = (String) req.get("fullName");
        String phone = (String) req.get("phone");

        User user = new User(email, passwordEncoder.encode(password), fullName, phone, "CUSTOMER", true);
        user = userRepository.save(user);

        Donor donor = new Donor();
        donor.setUserId(user.getId());
        donor.setBloodGroup((String) req.get("bloodGroup"));
        donor.setDateOfBirth(LocalDate.parse((String) req.get("dateOfBirth")));
        donor.setGender((String) req.get("gender"));
        donor.setCity((String) req.get("city"));
        donor.setPincode((String) req.get("pincode"));
        donor.setIsEligible(true);
        donorRepository.save(donor);

        return user;
    }

    @Transactional
    public User registerHospital(Map<String, Object> req) {
        String email = (String) req.get("email");
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email is already registered");
        }

        String licenseNumber = (String) req.get("licenseNumber");
        if (hospitalRepository.existsByLicenseNumber(licenseNumber)) {
            throw new RuntimeException("Hospital license number is already registered");
        }

        String password = (String) req.get("password");
        String hospitalName = (String) req.get("hospitalName");
        String phone = (String) req.get("contactPhone");

        // Hospital is registered with is_active = false until Admin approves
        User user = new User(email, passwordEncoder.encode(password), hospitalName, phone, "HOSPITAL", false);
        user = userRepository.save(user);

        Hospital hospital = new Hospital();
        hospital.setUserId(user.getId());
        hospital.setHospitalName(hospitalName);
        hospital.setLicenseNumber(licenseNumber);
        hospital.setAddress((String) req.get("address"));
        hospital.setCity((String) req.get("city"));
        hospital.setPincode((String) req.get("pincode"));
        hospital.setContactPerson((String) req.get("contactPerson"));
        hospital.setContactPhone(phone);
        hospital.setVerificationStatus("PENDING");
        hospital = hospitalRepository.save(hospital);

        // Pre-populate inventory for all 8 standard blood groups with 0 units
        String[] groups = { "A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-" };
        for (String bg : groups) {
            inventoryRepository.save(new BloodInventory(hospital.getId(), bg, 0));
        }

        return user;
    }

    @Transactional
    public void adminResetPassword(Long userId, String email, String newPassword, String confirmPassword) {
        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new RuntimeException("New password is required");
        }
        if (confirmPassword == null || confirmPassword.trim().isEmpty()) {
            throw new RuntimeException("Confirm password is required");
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new RuntimeException("Passwords do not match");
        }
        if (newPassword.length() < 6) {
            throw new RuntimeException("Password must be at least 6 characters long");
        }

        User targetUser = null;
        if (userId != null && userId > 0) {
            targetUser = userRepository.findById(userId).orElse(null);
        }

        if (targetUser == null && email != null && !email.trim().isEmpty()) {
            String cleanEmail = email.trim();
            targetUser = userRepository.findByEmailIgnoreCase(cleanEmail)
                    .orElse(userRepository.findByEmail(cleanEmail).orElse(null));
        }

        // If target is primary hardcoded admin (admin@bloodconnect.com) and not in DB yet
        if (targetUser == null) {
            if ((email != null && ADMIN_EMAIL.equalsIgnoreCase(email.trim())) || (userId != null && userId == 0L)) {
                Optional<User> adminOpt = userRepository.findByEmail(ADMIN_EMAIL);
                if (adminOpt.isPresent()) {
                    targetUser = adminOpt.get();
                } else {
                    targetUser = new User(
                        ADMIN_EMAIL,
                        passwordEncoder.encode(newPassword),
                        "System Administrator",
                        "9876543210",
                        "ADMIN",
                        true
                    );
                    userRepository.save(targetUser);
                    return;
                }
            }
        }

        if (targetUser == null) {
            throw new RuntimeException("User account not found");
        }

        targetUser.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(targetUser);
    }
}
