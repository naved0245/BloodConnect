package com.bloodconnect.repository;

import com.bloodconnect.model.Donor;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface DonorRepository extends JpaRepository<Donor, Long> {
    Optional<Donor> findByUserId(Long userId);
}
