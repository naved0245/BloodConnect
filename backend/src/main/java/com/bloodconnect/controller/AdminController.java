package com.bloodconnect.controller;

import com.bloodconnect.model.*;
import com.bloodconnect.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DonorRepository donorRepository;

    @Autowired
    private HospitalRepository hospitalRepository;

    @Autowired
    private BloodInventoryRepository inventoryRepository;

    @Autowired
    private BloodRequestRepository requestRepository;

    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalDonors", donorRepository.count());
        stats.put("approvedHospitals", hospitalRepository.countByVerificationStatus("APPROVED"));
        stats.put("pendingHospitals", hospitalRepository.countByVerificationStatus("PENDING"));
        stats.put("totalBloodUnits", inventoryRepository.getTotalUnitsAvailable());
        stats.put("openRequests", requestRepository.countByStatus("PENDING"));
        return ResponseEntity.ok(Map.of("success", true, "data", stats));
    }

    @GetMapping("/hospitals")
    public ResponseEntity<?> getAllHospitals() {
        List<Hospital> hospitals = hospitalRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Hospital h : hospitals) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", h.getId());
            map.put("userId", h.getUserId());
            map.put("hospitalName", h.getHospitalName());
            map.put("licenseNumber", h.getLicenseNumber());
            map.put("city", h.getCity());
            map.put("address", h.getAddress());
            map.put("contactPerson", h.getContactPerson());
            map.put("contactPhone", h.getContactPhone());
            map.put("verificationStatus", h.getVerificationStatus());
            map.put("verifiedAt", h.getVerifiedAt());

            userRepository.findById(h.getUserId()).ifPresent(u -> {
                map.put("email", u.getEmail());
                map.put("isActive", u.getIsActive());
            });
            result.add(map);
        }
        return ResponseEntity.ok(Map.of("success", true, "data", result));
    }

    @PutMapping("/hospitals/{id}/verify")
    @Transactional
    public ResponseEntity<?> verifyHospital(@PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            Hospital hospital = hospitalRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Hospital not found"));
            String action = body.get("action"); // "APPROVE" or "REJECT"

            User user = userRepository.findById(hospital.getUserId())
                    .orElseThrow(() -> new RuntimeException("Associated user account not found"));

            if ("APPROVE".equalsIgnoreCase(action)) {
                hospital.setVerificationStatus("APPROVED");
                hospital.setVerifiedAt(LocalDateTime.now());
                user.setIsActive(true);
            } else {
                hospital.setVerificationStatus("REJECTED");
                user.setIsActive(false);
            }

            hospitalRepository.save(hospital);
            userRepository.save(user);

            return ResponseEntity.ok(Map.of(
                "success", true, 
                "message", "Hospital " + ("APPROVE".equalsIgnoreCase(action) ? "approved" : "rejected") + " successfully."
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        List<User> users = userRepository.findAll();
        return ResponseEntity.ok(Map.of("success", true, "data", users));
    }

    @PutMapping("/users/{id}/toggle-status")
    public ResponseEntity<?> toggleUserStatus(@PathVariable Long id) {
        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if ("ADMIN".equals(user.getRole())) {
                throw new RuntimeException("Cannot deactivate primary administrator");
            }

            user.setIsActive(!user.getIsActive());
            userRepository.save(user);

            return ResponseEntity.ok(Map.of(
                "success", true, 
                "message", "User status changed to " + (user.getIsActive() ? "ACTIVE" : "INACTIVE"),
                "isActive", user.getIsActive()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/inventory")
    public ResponseEntity<?> getAllInventory() {
        List<BloodInventory> inventories = inventoryRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();
        for (BloodInventory inv : inventories) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", inv.getId());
            map.put("hospitalId", inv.getHospitalId());
            map.put("bloodGroup", inv.getBloodGroup());
            map.put("unitsAvailable", inv.getUnitsAvailable());
            map.put("lastUpdated", inv.getLastUpdated());

            hospitalRepository.findById(inv.getHospitalId()).ifPresent(h -> {
                map.put("hospitalName", h.getHospitalName());
                map.put("city", h.getCity());
            });
            result.add(map);
        }
        return ResponseEntity.ok(Map.of("success", true, "data", result));
    }
}
