package com.bunsen.api.aftercare.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "motorcycles")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Motorcycle {
    @Id
    @Column(name = "motorcycle_id", length = 36, nullable = false)
    private String motorcycleId;
    @Column(name = "qr_code", length = 255, nullable = false, unique = true)
    private String qrCode;
    @Column(name = "model", length = 100, nullable = false)
    private String model;
    @Column(name = "owner_name", length = 100)
    private String ownerName;
    @Column(name = "owner_phone", length = 20)
    private String ownerPhone;
    @Column(name = "owner_email", length = 255)
    private String ownerEmail;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private MotorcycleStatus status = MotorcycleStatus.ACTIVE;
    @Column(name = "last_service_date")
    private LocalDate lastServiceDate;
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = MotorcycleStatus.ACTIVE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum MotorcycleStatus {
        ACTIVE,
        INACTIVE,
        IN_SERVICE
    }
}