package com.bunsen.api.aftercare.model;

import com.bunsen.api.aftercare.model.base.BaseEntity;
import com.bunsen.api.aftercare.model.embedded.OwnerInfo;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "motorcycles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Motorcycle extends BaseEntity {

    @Column(name = "qr_code", length = 255, nullable = false, unique = true)
    private String qrCode;

    @Column(name = "model", length = 100, nullable = false)
    private String model;

    @Column(name = "plate_number", length = 100)
    private String plateNumber;

    @Embedded
    private OwnerInfo owner;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private MotorcycleStatus status = MotorcycleStatus.ACTIVE;

    @Column(name = "last_service_date")
    private LocalDateTime lastServiceDate;

    public enum MotorcycleStatus {
        ACTIVE,
        INACTIVE,
        IN_SERVICE
    }

    @Override
    protected void onCreate() {
        super.onCreate();
        if (status == null) {
            status = MotorcycleStatus.ACTIVE;
        }
    }
}