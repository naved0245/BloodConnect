package com.bloodconnect.service;

import com.bloodconnect.model.*;
import com.bloodconnect.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class HospitalService {

    @Autowired
    private HospitalRepository hospitalRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DonationRepository donationRepository;

    @Autowired
    private DonorRepository donorRepository;

    public Hospital getHospitalByUserId(Long userId) {
        return hospitalRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Hospital record not found for user"));
    }

    public Map<String, Object> getProfile(Long userId) {
        Hospital hospital = getHospitalByUserId(userId);
        User user = userRepository.findById(userId).orElseThrow();

        Map<String, Object> map = new HashMap<>();
        map.put("id", hospital.getId());
        map.put("userId", user.getId());
        map.put("email", user.getEmail());
        map.put("hospitalName", hospital.getHospitalName());
        map.put("licenseNumber", hospital.getLicenseNumber());
        map.put("address", hospital.getAddress());
        map.put("city", hospital.getCity());
        map.put("pincode", hospital.getPincode());
        map.put("contactPerson", hospital.getContactPerson());
        map.put("contactPhone", hospital.getContactPhone());
        map.put("verificationStatus", hospital.getVerificationStatus());
        map.put("verifiedAt", hospital.getVerifiedAt());
        return map;
    }

    @Transactional
    public void updateProfile(Long userId, Map<String, Object> req) {
        Hospital hospital = getHospitalByUserId(userId);
        if (req.containsKey("hospitalName")) hospital.setHospitalName((String) req.get("hospitalName"));
        if (req.containsKey("address")) hospital.setAddress((String) req.get("address"));
        if (req.containsKey("city")) hospital.setCity((String) req.get("city"));
        if (req.containsKey("pincode")) hospital.setPincode((String) req.get("pincode"));
        if (req.containsKey("contactPerson")) hospital.setContactPerson((String) req.get("contactPerson"));
        if (req.containsKey("contactPhone")) hospital.setContactPhone((String) req.get("contactPhone"));
        hospitalRepository.save(hospital);
    }

    public List<Map<String, Object>> getAppointments(Long hospitalId) {
        List<Donation> donations = donationRepository.findByHospitalIdOrderByScheduledDateDesc(hospitalId);
        List<Map<String, Object>> result = new ArrayList<>();

        for (Donation d : donations) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", d.getId());
            map.put("donorId", d.getDonorId());
            map.put("bloodGroup", d.getBloodGroup());
            map.put("scheduledDate", d.getScheduledDate());
            map.put("timeSlot", d.getTimeSlot());
            map.put("status", d.getStatus());
            map.put("remarks", d.getRemarks());
            map.put("createdAt", d.getCreatedAt());

            donorRepository.findById(d.getDonorId()).ifPresent(donor -> {
                userRepository.findById(donor.getUserId()).ifPresent(u -> {
                    map.put("donorName", u.getFullName());
                    map.put("donorPhone", u.getPhone());
                    map.put("donorEmail", u.getEmail());
                });
            });
            result.add(map);
        }
        return result;
    }

    @Transactional
    public void updateAppointmentStatus(Long hospitalId, Long donationId, String status, String remarks) {
        Donation donation = donationRepository.findById(donationId)
                .orElseThrow(() -> new RuntimeException("Donation record not found"));

        if (!donation.getHospitalId().equals(hospitalId)) {
            throw new RuntimeException("Unauthorized hospital access to this appointment");
        }

        if ("COMPLETED".equals(donation.getStatus())) {
            throw new RuntimeException("Completed donations cannot be altered");
        }

        donation.setStatus(status);
        if (remarks != null && !remarks.isBlank()) {
            donation.setRemarks(remarks);
        }
        donationRepository.save(donation);
    }

    public List<Hospital> getApprovedHospitals() {
        return hospitalRepository.findByVerificationStatus("APPROVED");
    }
}
