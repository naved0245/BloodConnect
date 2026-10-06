package com.bloodconnect.service;

import com.bloodconnect.model.*;
import com.bloodconnect.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DonorRepository donorRepository;

    @Autowired
    private HospitalRepository hospitalRepository;

    @Autowired
    private BloodInventoryRepository inventoryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public Map<String, Object> login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new RuntimeException("Invalid email or password");
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
        String[] groups = {"A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"};
        for (String bg : groups) {
            inventoryRepository.save(new BloodInventory(hospital.getId(), bg, 0));
        }

        return user;
    }
}
