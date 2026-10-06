package com.bloodconnect.repository;

import com.bloodconnect.model.BloodRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BloodRequestRepository extends JpaRepository<BloodRequest, Long> {
    List<BloodRequest> findByHospitalIdOrderByCreatedAtDesc(Long hospitalId);
    List<BloodRequest> findByStatusOrderByCreatedAtDesc(String status);
    long countByStatus(String status);
}
