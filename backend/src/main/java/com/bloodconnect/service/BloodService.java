package com.bloodconnect.service;

import com.bloodconnect.model.*;
import com.bloodconnect.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class BloodService {

    @Autowired
    private BloodInventoryRepository inventoryRepository;

    @Autowired
    private BloodRequestRepository requestRepository;

    @Autowired
    private DonationRepository donationRepository;

    @Autowired
    private DonorRepository donorRepository;

    @Autowired
    private HospitalRepository hospitalRepository;

    public List<BloodInventory> getHospitalInventory(Long hospitalId) {
        String[] groups = {"A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"};
        List<BloodInventory> list = new ArrayList<>();
        for (String bg : groups) {
            BloodInventory inv = inventoryRepository.findByHospitalIdAndBloodGroup(hospitalId, bg)
                    .orElseGet(() -> inventoryRepository.save(new BloodInventory(hospitalId, bg, 0)));
            list.add(inv);
        }
        return list;
    }

    @Transactional
    public BloodInventory adjustInventory(Long hospitalId, String bloodGroup, int delta, String remark) {
        BloodInventory inv = inventoryRepository.findByHospitalIdAndBloodGroup(hospitalId, bloodGroup)
                .orElseGet(() -> new BloodInventory(hospitalId, bloodGroup, 0));

        int newTotal = inv.getUnitsAvailable() + delta;
        if (newTotal < 0) {
            throw new RuntimeException("Inventory cannot be negative. Current stock: " 
                    + inv.getUnitsAvailable() + ", requested change: " + delta);
        }

        inv.setUnitsAvailable(newTotal);
        inv.setLastUpdated(LocalDateTime.now());
        return inventoryRepository.save(inv);
    }

    @Transactional
    public Donation completeDonation(Long hospitalId, Long donationId, String remarks) {
        Donation donation = donationRepository.findById(donationId)
                .orElseThrow(() -> new RuntimeException("Donation record not found"));

        if (!donation.getHospitalId().equals(hospitalId)) {
            throw new RuntimeException("Unauthorized hospital access to this appointment");
        }

        if ("COMPLETED".equals(donation.getStatus())) {
            throw new RuntimeException("Donation has already been completed.");
        }

        if ("CANCELLED".equals(donation.getStatus())) {
            throw new RuntimeException("Cannot complete a cancelled donation.");
        }

        final String bloodGroup = donation.getBloodGroup();

        // 1. Mark donation completed
        donation.setStatus("COMPLETED");
        if (remarks != null && !remarks.isBlank()) {
            donation.setRemarks(remarks);
        }
        donation = donationRepository.save(donation);

        // 2. Increase inventory by 1 unit
        BloodInventory inv = inventoryRepository.findByHospitalIdAndBloodGroup(hospitalId, bloodGroup)
                .orElseGet(() -> new BloodInventory(hospitalId, bloodGroup, 0));
        inv.setUnitsAvailable(inv.getUnitsAvailable() + 1);
        inv.setLastUpdated(LocalDateTime.now());
        inventoryRepository.save(inv);

        // 3. Update donor cooldown & eligibility
        donorRepository.findById(donation.getDonorId()).ifPresent(donor -> {
            donor.setLastDonationDate(LocalDate.now());
            donor.setIsEligible(false);
            donorRepository.save(donor);
        });

        return donation;
    }

    @Transactional
    public BloodRequest createBloodRequest(Long hospitalId, String bloodGroup, int requiredUnits, String urgency, String notes) {
        if (requiredUnits <= 0) {
            throw new RuntimeException("Required units must be at least 1");
        }

        BloodRequest req = new BloodRequest();
        req.setHospitalId(hospitalId);
        req.setBloodGroup(bloodGroup);
        req.setRequiredUnits(requiredUnits);
        req.setFulfilledUnits(0);
        req.setUrgencyLevel(urgency != null ? urgency : "ROUTINE");
        req.setStatus("PENDING");
        req.setNotes(notes);
        return requestRepository.save(req);
    }

    public List<BloodRequest> getHospitalRequests(Long hospitalId) {
        return requestRepository.findByHospitalIdOrderByCreatedAtDesc(hospitalId);
    }

    public List<Map<String, Object>> getActivePublicRequests() {
        List<BloodRequest> requests = requestRepository.findByStatusOrderByCreatedAtDesc("PENDING");
        List<Map<String, Object>> result = new ArrayList<>();
        for (BloodRequest r : requests) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", r.getId());
            map.put("bloodGroup", r.getBloodGroup());
            map.put("requiredUnits", r.getRequiredUnits());
            map.put("urgencyLevel", r.getUrgencyLevel());
            map.put("status", r.getStatus());
            map.put("notes", r.getNotes());
            map.put("createdAt", r.getCreatedAt());

            hospitalRepository.findById(r.getHospitalId()).ifPresent(h -> {
                map.put("hospitalName", h.getHospitalName());
                map.put("hospitalCity", h.getCity());
                map.put("hospitalPhone", h.getContactPhone());
            });
            result.add(map);
        }
        return result;
    }

    @Transactional
    public BloodRequest fulfillRequest(Long hospitalId, Long requestId) {
        BloodRequest req = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Blood request not found"));

        if (!req.getHospitalId().equals(hospitalId)) {
            throw new RuntimeException("Unauthorized hospital access to this blood request");
        }

        if ("FULFILLED".equals(req.getStatus())) {
            throw new RuntimeException("This request has already been fulfilled");
        }

        if ("REJECTED".equals(req.getStatus()) || "CANCELLED".equals(req.getStatus())) {
            throw new RuntimeException("Cannot fulfill a " + req.getStatus() + " request");
        }

        BloodInventory inv = inventoryRepository.findByHospitalIdAndBloodGroup(hospitalId, req.getBloodGroup())
                .orElseGet(() -> new BloodInventory(hospitalId, req.getBloodGroup(), 0));

        if (inv.getUnitsAvailable() < req.getRequiredUnits()) {
            throw new RuntimeException("Insufficient inventory to fulfill request. Available units: " 
                    + inv.getUnitsAvailable() + ", Required units: " + req.getRequiredUnits());
        }

        // Deduct inventory atomically
        inv.setUnitsAvailable(inv.getUnitsAvailable() - req.getRequiredUnits());
        inv.setLastUpdated(LocalDateTime.now());
        inventoryRepository.save(inv);

        req.setFulfilledUnits(req.getRequiredUnits());
        req.setStatus("FULFILLED");
        return requestRepository.save(req);
    }

    @Transactional
    public BloodRequest updateRequestStatus(Long hospitalId, Long requestId, String status) {
        BloodRequest req = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Blood request not found"));

        if (!req.getHospitalId().equals(hospitalId)) {
            throw new RuntimeException("Unauthorized hospital access to this blood request");
        }

        if ("FULFILLED".equals(req.getStatus())) {
            throw new RuntimeException("Cannot change status of an already fulfilled request");
        }

        req.setStatus(status);
        return requestRepository.save(req);
    }
}
