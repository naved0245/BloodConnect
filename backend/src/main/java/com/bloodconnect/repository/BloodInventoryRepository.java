package com.bloodconnect.repository;

import com.bloodconnect.model.BloodInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface BloodInventoryRepository extends JpaRepository<BloodInventory, Long> {
    List<BloodInventory> findByHospitalId(Long hospitalId);
    Optional<BloodInventory> findByHospitalIdAndBloodGroup(Long hospitalId, String bloodGroup);

    @Query("SELECT COALESCE(SUM(b.unitsAvailable), 0) FROM BloodInventory b")
    long getTotalUnitsAvailable();
}
