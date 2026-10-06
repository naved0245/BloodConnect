package com.bloodconnect.controller;

import com.bloodconnect.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/donor")
@CrossOrigin(origins = "*")
public class DonorController {

    @Autowired
    private DonorService donorService;

    @Autowired
    private HospitalService hospitalService;

    @Autowired
    private BloodService bloodService;

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@RequestParam Long userId) {
        try {
            return ResponseEntity.ok(Map.of("success", true, "data", donorService.getProfile(userId)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestParam Long userId, @RequestBody Map<String, Object> body) {
        try {
            donorService.updateProfile(userId, body);
            return ResponseEntity.ok(Map.of("success", true, "message", "Profile updated successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/hospitals")
    public ResponseEntity<?> getHospitals() {
        return ResponseEntity.ok(Map.of("success", true, "data", hospitalService.getApprovedHospitals()));
    }

    @PostMapping("/schedule")
    public ResponseEntity<?> scheduleDonation(@RequestBody Map<String, Object> body) {
        try {
            Long userId = Long.valueOf(body.get("userId").toString());
            Long hospitalId = Long.valueOf(body.get("hospitalId").toString());
            LocalDate scheduledDate = LocalDate.parse(body.get("scheduledDate").toString());
            String timeSlot = body.get("timeSlot").toString();

            var donation = donorService.scheduleDonation(userId, hospitalId, scheduledDate, timeSlot);
            return ResponseEntity.ok(Map.of("success", true, "message", "Appointment scheduled successfully!", "data", donation));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/donations")
    public ResponseEntity<?> getDonations(@RequestParam Long userId) {
        try {
            return ResponseEntity.ok(Map.of("success", true, "data", donorService.getMyDonations(userId)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PutMapping("/donations/{id}/cancel")
    public ResponseEntity<?> cancelDonation(@PathVariable Long id, @RequestParam Long userId) {
        try {
            donorService.cancelDonation(userId, id);
            return ResponseEntity.ok(Map.of("success", true, "message", "Appointment cancelled successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/blood-requests")
    public ResponseEntity<?> getBloodRequests() {
        return ResponseEntity.ok(Map.of("success", true, "data", bloodService.getActivePublicRequests()));
    }
}
