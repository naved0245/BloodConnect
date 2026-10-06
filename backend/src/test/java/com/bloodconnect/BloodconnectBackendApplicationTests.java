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
}
