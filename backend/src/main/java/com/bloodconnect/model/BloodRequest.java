package com.bloodconnect.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "blood_requests")
public class BloodRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hospital_id", nullable = false)
    private Long hospitalId;

    @Column(name = "blood_group", nullable = false, length = 5)
    private String bloodGroup;

    @Column(name = "required_units", nullable = false)
    private Integer requiredUnits;

    @Column(name = "fulfilled_units", nullable = false)
    private Integer fulfilledUnits = 0;

    @Column(name = "urgency_level", nullable = false, length = 20)
    private String urgencyLevel; // ROUTINE, URGENT, CRITICAL

    @Column(nullable = false, length = 20)
    private String status = "PENDING"; // PENDING, APPROVED, FULFILLED, REJECTED, CANCELLED

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public BloodRequest() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getHospitalId() { return hospitalId; }
    public void setHospitalId(Long hospitalId) { this.hospitalId = hospitalId; }

    public String getBloodGroup() { return bloodGroup; }
    public void setBloodGroup(String bloodGroup) { this.bloodGroup = bloodGroup; }

    public Integer getRequiredUnits() { return requiredUnits; }
    public void setRequiredUnits(Integer requiredUnits) { this.requiredUnits = requiredUnits; }

    public Integer getFulfilledUnits() { return fulfilledUnits; }
    public void setFulfilledUnits(Integer fulfilledUnits) { this.fulfilledUnits = fulfilledUnits; }

    public String getUrgencyLevel() { return urgencyLevel; }
    public void setUrgencyLevel(String urgencyLevel) { this.urgencyLevel = urgencyLevel; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
