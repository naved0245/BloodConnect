package com.bloodconnect;

import com.bloodconnect.controller.AdminController;
import com.bloodconnect.model.*;
import com.bloodconnect.repository.*;
import com.bloodconnect.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
class BloodconnectBackendApplicationTests {

    @Autowired
    private AuthService authService;

    @Autowired
    private DonorService donorService;

    @Autowired
    private HospitalService hospitalService;

    @Autowired
    private BloodService bloodService;

    @Autowired
    private AdminController adminController;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HospitalRepository hospitalRepository;

    @Autowired
    private BloodInventoryRepository inventoryRepository;

    @Test
    void contextLoads() {
        assertNotNull(authService);
        assertNotNull(donorService);
        assertNotNull(hospitalService);
        assertNotNull(bloodService);
    }

    @Test
    void testActualMySqlConnection() {
        try (java.sql.Connection conn = java.sql.DriverManager.getConnection("jdbc:mysql://localhost:3306/?useSSL=false&allowPublicKeyRetrieval=true", "root", "root")) {
            System.out.println(">>> MYSQL CONNECTED SUCCESSFULLY WITH USER ROOT AND PASSWORD ROOT!");
        } catch (Exception e) {
            System.out.println(">>> MYSQL CONNECTION RESULT: " + e.getMessage());
        }
    }

    @Test
    void testCustomerRegistrationAndLogin() {
        Map<String, Object> req = Map.of(
            "fullName", "Rahul Sharma",
            "email", "rahul.test@bloodconnect.com",
            "password", "Rahul@123",
            "phone", "9876543211",
            "bloodGroup", "O+",
            "dateOfBirth", "2000-01-15",
            "gender", "Male",
            "city", "Mumbai",
            "pincode", "400001"
        );

        User user = authService.registerCustomer(req);
        assertNotNull(user.getId());
        assertEquals("CUSTOMER", user.getRole());

        // Test login
        Map<String, Object> loginData = authService.login("rahul.test@bloodconnect.com", "Rahul@123");
        assertEquals("rahul.test@bloodconnect.com", loginData.get("email"));
        assertEquals("CUSTOMER", loginData.get("role"));
    }

    @Test
    void testHospitalRegistrationAndApprovalFlow() {
        Map<String, Object> req = Map.of(
            "hospitalName", "Apex Care Hospital",
            "licenseNumber", "APEX-LIC-999",
            "email", "apex@hospital.org",
            "password", "Apex@123",
            "contactPhone", "9123456780",
            "contactPerson", "Dr. Mehta",
            "address", "10 Marine Drive",
            "city", "Mumbai",
            "pincode", "400020"
        );

        User hospUser = authService.registerHospital(req);
        assertNotNull(hospUser.getId());
        assertFalse(hospUser.getIsActive()); // Pending admin approval

        // Unapproved hospital CANNOT login
        assertThrows(RuntimeException.class, () -> {
            authService.login("apex@hospital.org", "Apex@123");
        });

        // Admin approves hospital
        Hospital hosp = hospitalRepository.findByUserId(hospUser.getId()).orElseThrow();
        adminController.verifyHospital(hosp.getId(), Map.of("action", "APPROVE"));

        // Now hospital CAN login
        Map<String, Object> loginData = authService.login("apex@hospital.org", "Apex@123");
        assertEquals("HOSPITAL", loginData.get("role"));
        assertNotNull(loginData.get("hospitalId"));
    }

    @Test
    void testDonationFlowAndInventoryIncrement() {
        // Register hospital and approve
        User hospUser = authService.registerHospital(Map.of(
            "hospitalName", "City Blood Center",
            "licenseNumber", "CITY-LIC-888",
            "email", "citybc@hospital.org",
            "password", "City@123",
            "contactPhone", "9123456781",
            "contactPerson", "Dr. Rao",
            "address", "55 Station Rd",
            "city", "Pune",
            "pincode", "411001"
        ));
        Hospital hosp = hospitalRepository.findByUserId(hospUser.getId()).orElseThrow();
        adminController.verifyHospital(hosp.getId(), Map.of("action", "APPROVE"));

        // Register donor
        User donorUser = authService.registerCustomer(Map.of(
            "fullName", "Amit Patel",
            "email", "amit.patel@test.com",
            "password", "Amit@123",
            "phone", "9876543212",
            "bloodGroup", "B+",
            "dateOfBirth", "1998-05-20",
            "gender", "Male",
            "city", "Pune",
            "pincode", "411001"
        ));

        // Schedule donation
        Donation donation = donorService.scheduleDonation(
            donorUser.getId(), hosp.getId(), LocalDate.now(), "10:00 AM - 12:00 PM"
        );
        assertEquals("SCHEDULED", donation.getStatus());

        // Check initial inventory of B+ is 0
        BloodInventory invBefore = inventoryRepository.findByHospitalIdAndBloodGroup(hosp.getId(), "B+").orElseThrow();
        assertEquals(0, invBefore.getUnitsAvailable());

        // Hospital completes donation
        Donation completed = bloodService.completeDonation(hosp.getId(), donation.getId(), "Successfully collected 1 unit");
        assertEquals("COMPLETED", completed.getStatus());

        // Inventory must now be +1 unit
        BloodInventory invAfter = inventoryRepository.findByHospitalIdAndBloodGroup(hosp.getId(), "B+").orElseThrow();
        assertEquals(1, invAfter.getUnitsAvailable());

        // Donor must now be in cooldown
        Map<String, Object> donorProfile = donorService.getProfile(donorUser.getId());
        assertFalse((Boolean) donorProfile.get("isEligible"));
    }

    @Test
    void testBloodRequestFulfillmentAndInventoryDeduction() {
        // Setup hospital with inventory
        User hospUser = authService.registerHospital(Map.of(
            "hospitalName", "Metro Life Care",
            "licenseNumber", "METRO-LIC-777",
            "email", "metro@hospital.org",
            "password", "Metro@123",
            "contactPhone", "9123456782",
            "contactPerson", "Dr. Sen",
            "address", "77 Central Ave",
            "city", "Delhi",
            "pincode", "110001"
        ));
        Hospital hosp = hospitalRepository.findByUserId(hospUser.getId()).orElseThrow();
        adminController.verifyHospital(hosp.getId(), Map.of("action", "APPROVE"));

        // Adjust inventory to 5 units of A+
        bloodService.adjustInventory(hosp.getId(), "A+", 5, "Initial batch");
        assertEquals(5, inventoryRepository.findByHospitalIdAndBloodGroup(hosp.getId(), "A+").orElseThrow().getUnitsAvailable());

        // Create blood request for 3 units
        BloodRequest req = bloodService.createBloodRequest(hosp.getId(), "A+", 3, "URGENT", "ICU demand");
        assertEquals("PENDING", req.getStatus());

        // Fulfill request
        BloodRequest fulfilled = bloodService.fulfillRequest(hosp.getId(), req.getId());
        assertEquals("FULFILLED", fulfilled.getStatus());
        assertEquals(3, fulfilled.getFulfilledUnits());

        // Inventory must now be 5 - 3 = 2 units
        assertEquals(2, inventoryRepository.findByHospitalIdAndBloodGroup(hosp.getId(), "A+").orElseThrow().getUnitsAvailable());

        // Attempting to deduct more than available (e.g. 5 units) must throw exception and protect inventory
        BloodRequest excessReq = bloodService.createBloodRequest(hosp.getId(), "A+", 5, "CRITICAL", "High demand");
        assertThrows(RuntimeException.class, () -> {
            bloodService.fulfillRequest(hosp.getId(), excessReq.getId());
        });

        // Inventory must still be 2 units (non-negative protection)
        assertEquals(2, inventoryRepository.findByHospitalIdAndBloodGroup(hosp.getId(), "A+").orElseThrow().getUnitsAvailable());
    }

    @Test
    void testHardcodedPrimaryAdminLogin() {
        Map<String, Object> admin = authService.login("admin@bloodconnect.com", "Admin@123");
        assertNotNull(admin);
        assertEquals("admin@bloodconnect.com", admin.get("email"));
        assertEquals("ADMIN", admin.get("role"));

        // Test with explicit role = "ADMIN", mixed-case and whitespace
        Map<String, Object> adminWithRole = authService.login("  Admin@BloodConnect.com  ", "Admin@123", "ADMIN");
        assertNotNull(adminWithRole);
        assertEquals("ADMIN", adminWithRole.get("role"));
    }

    @Test
    void testPrimaryAdminRoleMismatch() {
        assertThrows(RuntimeException.class, () -> {
            authService.login("admin@bloodconnect.com", "Admin@123", "CUSTOMER");
        });
    }

    @Test
    void testInvalidPrimaryAdminCredentials() {
        assertThrows(RuntimeException.class, () -> {
            authService.login("admin@bloodconnect.com", "WrongPassword@123");
        });
    }

    @Test
    void testCreateAdminAndDatabaseAdminLogin() {
        Map<String, Object> newAdminReq = Map.of(
            "fullName", "Sub Administrator",
            "email", "testadmin@bloodconnect.com",
            "password", "Test@123",
            "confirmPassword", "Test@123"
        );

        // 1. Primary admin creates database admin
        User created = authService.createAdmin(newAdminReq, "admin@bloodconnect.com");
        assertNotNull(created.getId());
        assertEquals("testadmin@bloodconnect.com", created.getEmail());
        assertEquals("ADMIN", created.getRole());
        assertTrue(created.getPasswordHash().startsWith("$2a$")); // BCrypt hashed

        // 2. Newly created admin logs in successfully
        Map<String, Object> loginData = authService.login("testadmin@bloodconnect.com", "Test@123");
        assertEquals("testadmin@bloodconnect.com", loginData.get("email"));
        assertEquals("ADMIN", loginData.get("role"));

        // 3. Database admin can also create another admin
        Map<String, Object> secondAdminReq = Map.of(
            "fullName", "Second Administrator",
            "email", "secondadmin@bloodconnect.com",
            "password", "Second@123",
            "confirmPassword", "Second@123"
        );
        User secondCreated = authService.createAdmin(secondAdminReq, "testadmin@bloodconnect.com");
        assertNotNull(secondCreated.getId());
        assertEquals("secondadmin@bloodconnect.com", secondCreated.getEmail());
    }

    @Test
    void testDuplicateAdminEmail() {
        // Try creating with primary admin email
        assertThrows(RuntimeException.class, () -> {
            authService.createAdmin(Map.of(
                "fullName", "Duplicate Admin",
                "email", "admin@bloodconnect.com",
                "password", "Pass@123",
                "confirmPassword", "Pass@123"
            ), "admin@bloodconnect.com");
        });
    }

    @Test
    void testUnauthorizedAdminCreation() {
        Map<String, Object> req = Map.of(
            "fullName", "Hack Admin",
            "email", "hack@bloodconnect.com",
            "password", "Hack@123",
            "confirmPassword", "Hack@123"
        );

        // Unauthenticated (null or empty requester)
        assertThrows(RuntimeException.class, () -> {
            authService.createAdmin(req, null);
        });

        assertThrows(RuntimeException.class, () -> {
            authService.createAdmin(req, "");
        });

        // Customer / unauthorized user attempting admin creation
        User customer = authService.registerCustomer(Map.of(
            "fullName", "Normal Donor",
            "email", "donor.attacker@bloodconnect.com",
            "password", "Donor@123",
            "phone", "9876543299",
            "bloodGroup", "O-",
            "dateOfBirth", "1999-01-01",
            "gender", "Female",
            "city", "Delhi",
            "pincode", "110001"
        ));

        assertThrows(RuntimeException.class, () -> {
            authService.createAdmin(req, customer.getEmail());
        });
    }
}
