package com.bloodconnect.service;

import com.bloodconnect.model.*;
import com.bloodconnect.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class DonorService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DonorRepository donorRepository;

    @Autowired
    private HospitalRepository hospitalRepository;

    @Autowired
    private DonationRepository donationRepository;

    public Map<String, Object> getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Donor donor = donorRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Donor profile not found"));

        boolean eligible = checkEligibility(donor);

        Map<String, Object> map = new HashMap<>();
        map.put("userId", user.getId());
        map.put("donorId", donor.getId());
        map.put("fullName", user.getFullName());
        map.put("email", user.getEmail());
        map.put("phone", user.getPhone());
        map.put("bloodGroup", donor.getBloodGroup());
        map.put("dateOfBirth", donor.getDateOfBirth());
        map.put("gender", donor.getGender());
        map.put("city", donor.getCity());
        map.put("pincode", donor.getPincode());
        map.put("lastDonationDate", donor.getLastDonationDate());
        map.put("isEligible", eligible);

        if (donor.getLastDonationDate() != null) {
            LocalDate nextEligible = donor.getLastDonationDate().plusDays(90);
            map.put("nextEligibleDate", nextEligible);
        } else {
            map.put("nextEligibleDate", LocalDate.now());
        }

        return map;
    }

    @Transactional
    public void updateProfile(Long userId, Map<String, Object> req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Donor donor = donorRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Donor profile not found"));

        if (req.containsKey("fullName")) user.setFullName((String) req.get("fullName"));
        if (req.containsKey("phone")) user.setPhone((String) req.get("phone"));
        userRepository.save(user);

        if (req.containsKey("city")) donor.setCity((String) req.get("city"));
        if (req.containsKey("pincode")) donor.setPincode((String) req.get("pincode"));
        donorRepository.save(donor);
    }

    public boolean checkEligibility(Donor donor) {
        if (donor.getDateOfBirth() != null) {
            int age = Period.between(donor.getDateOfBirth(), LocalDate.now()).getYears();
            if (age < 18) {
                donor.setIsEligible(false);
                donorRepository.save(donor);
                return false;
            }
        }

        if (donor.getLastDonationDate() != null) {
            long days = ChronoUnit.DAYS.between(donor.getLastDonationDate(), LocalDate.now());
            if (days < 90) {
                donor.setIsEligible(false);
                donorRepository.save(donor);
                return false;
            }
        }

        donor.setIsEligible(true);
        donorRepository.save(donor);
        return true;
    }

    @Transactional
    public Donation scheduleDonation(Long userId, Long hospitalId, LocalDate scheduledDate, String timeSlot) {
        Donor donor = donorRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Donor profile not found"));

        if (!checkEligibility(donor)) {
            throw new RuntimeException("You are not currently eligible to donate blood (90-day cooldown or age restriction).");
        }

        if (scheduledDate.isBefore(LocalDate.now())) {
            throw new RuntimeException("Appointment date must be today or a future date.");
        }

        Hospital hospital = hospitalRepository.findById(hospitalId)
                .orElseThrow(() -> new RuntimeException("Hospital not found"));
        if (!"APPROVED".equals(hospital.getVerificationStatus())) {
            throw new RuntimeException("Hospital is not verified for donations.");
        }

        Donation donation = new Donation();
        donation.setDonorId(donor.getId());
        donation.setHospitalId(hospitalId);
        donation.setBloodGroup(donor.getBloodGroup());
        donation.setScheduledDate(scheduledDate);
        donation.setTimeSlot(timeSlot);
        donation.setStatus("SCHEDULED");
        return donationRepository.save(donation);
    }

    public List<Map<String, Object>> getMyDonations(Long userId) {
        Donor donor = donorRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Donor profile not found"));
        List<Donation> list = donationRepository.findByDonorIdOrderByScheduledDateDesc(donor.getId());

        List<Map<String, Object>> result = new ArrayList<>();
        for (Donation d : list) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", d.getId());
            map.put("bloodGroup", d.getBloodGroup());
            map.put("scheduledDate", d.getScheduledDate());
            map.put("timeSlot", d.getTimeSlot());
            map.put("status", d.getStatus());
            map.put("remarks", d.getRemarks());
            map.put("createdAt", d.getCreatedAt());

            hospitalRepository.findById(d.getHospitalId()).ifPresent(h -> {
                map.put("hospitalName", h.getHospitalName());
                map.put("hospitalCity", h.getCity());
            });
            result.add(map);
        }
        return result;
    }

    @Transactional
    public void cancelDonation(Long userId, Long donationId) {
        Donor donor = donorRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Donor profile not found"));
        Donation donation = donationRepository.findById(donationId)
                .orElseThrow(() -> new RuntimeException("Donation appointment not found"));

        if (!donation.getDonorId().equals(donor.getId())) {
            throw new RuntimeException("Unauthorized action on this appointment.");
        }

        if ("COMPLETED".equals(donation.getStatus())) {
            throw new RuntimeException("Cannot cancel an already completed donation.");
        }

        donation.setStatus("CANCELLED");
        donationRepository.save(donation);
    }
}
