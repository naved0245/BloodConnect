package com.bloodconnect.repository;

import com.bloodconnect.model.Donation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DonationRepository extends JpaRepository<Donation, Long> {
    List<Donation> findByDonorIdOrderByScheduledDateDesc(Long donorId);
    List<Donation> findByHospitalIdOrderByScheduledDateDesc(Long hospitalId);
}
