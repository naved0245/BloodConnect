package com.bloodconnect.controller;

import com.bloodconnect.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/hospital")
@CrossOrigin(origins = "*")
public class HospitalController {

    @Autowired
    private HospitalService hospitalService;

    @Autowired
    private BloodService bloodService;

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@RequestParam Long userId) {
        try {
            return ResponseEntity.ok(Map.of("success", true, "data", hospitalService.getProfile(userId)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestParam Long userId, @RequestBody Map<String, Object> body) {
        try {
            hospitalService.updateProfile(userId, body);
            return ResponseEntity.ok(Map.of("success", true, "message", "Profile updated successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/appointments")
    public ResponseEntity<?> getAppointments(@RequestParam Long hospitalId) {
        try {
            return ResponseEntity.ok(Map.of("success", true, "data", hospitalService.getAppointments(hospitalId)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PutMapping("/appointments/{id}/status")
    public ResponseEntity<?> updateAppointmentStatus(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            Long hospitalId = Long.valueOf(body.get("hospitalId").toString());
            String status = body.get("status").toString();
            String remarks = body.containsKey("remarks") ? (String) body.get("remarks") : "";
            hospitalService.updateAppointmentStatus(hospitalId, id, status, remarks);
            return ResponseEntity.ok(Map.of("success", true, "message", "Appointment status updated to " + status));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PutMapping("/appointments/{id}/complete")
    public ResponseEntity<?> completeAppointment(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            Long hospitalId = Long.valueOf(body.get("hospitalId").toString());
            String remarks = body.containsKey("remarks") ? (String) body.get("remarks") : "";
            var donation = bloodService.completeDonation(hospitalId, id, remarks);
            return ResponseEntity.ok(Map.of(
                "success", true, 
                "message", "Donation marked COMPLETED! 1 unit added to " + donation.getBloodGroup() + " inventory.",
                "data", donation
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/inventory")
    public ResponseEntity<?> getInventory(@RequestParam Long hospitalId) {
        try {
            return ResponseEntity.ok(Map.of("success", true, "data", bloodService.getHospitalInventory(hospitalId)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PutMapping("/inventory/adjust")
    public ResponseEntity<?> adjustInventory(@RequestBody Map<String, Object> body) {
        try {
            Long hospitalId = Long.valueOf(body.get("hospitalId").toString());
            String bloodGroup = body.get("bloodGroup").toString();
            int change = Integer.parseInt(body.get("change").toString());
            String remark = body.containsKey("remark") ? body.get("remark").toString() : "";

            var inv = bloodService.adjustInventory(hospitalId, bloodGroup, change, remark);
            return ResponseEntity.ok(Map.of(
                "success", true, 
                "message", "Inventory updated for " + bloodGroup + ". Current units: " + inv.getUnitsAvailable(),
                "data", inv
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/requests")
    public ResponseEntity<?> getRequests(@RequestParam Long hospitalId) {
        try {
            return ResponseEntity.ok(Map.of("success", true, "data", bloodService.getHospitalRequests(hospitalId)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/requests")
    public ResponseEntity<?> createRequest(@RequestBody Map<String, Object> body) {
        try {
            Long hospitalId = Long.valueOf(body.get("hospitalId").toString());
            String bloodGroup = body.get("bloodGroup").toString();
            int requiredUnits = Integer.parseInt(body.get("requiredUnits").toString());
            String urgency = body.get("urgency").toString();
            String notes = body.containsKey("notes") ? (String) body.get("notes") : "";

            var req = bloodService.createBloodRequest(hospitalId, bloodGroup, requiredUnits, urgency, notes);
            return ResponseEntity.ok(Map.of("success", true, "message", "Blood request created successfully", "data", req));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PutMapping("/requests/{id}/fulfill")
    public ResponseEntity<?> fulfillRequest(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            Long hospitalId = Long.valueOf(body.get("hospitalId").toString());
            var req = bloodService.fulfillRequest(hospitalId, id);
            return ResponseEntity.ok(Map.of(
                "success", true, 
                "message", "Blood request fulfilled successfully! Units deducted from inventory.", 
                "data", req
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PutMapping("/requests/{id}/status")
    public ResponseEntity<?> updateRequestStatus(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            Long hospitalId = Long.valueOf(body.get("hospitalId").toString());
            String status = body.get("status").toString();
            var req = bloodService.updateRequestStatus(hospitalId, id, status);
            return ResponseEntity.ok(Map.of("success", true, "message", "Request status updated to " + status, "data", req));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
