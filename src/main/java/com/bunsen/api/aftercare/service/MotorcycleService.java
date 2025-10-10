package com.bunsen.api.aftercare.service;

import com.bunsen.api.aftercare.exception.ResourceNotFoundException;
import com.bunsen.api.aftercare.exception.ValidationException;
import com.bunsen.api.aftercare.model.Motorcycle;
import com.bunsen.api.aftercare.repository.MotorcycleRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class MotorcycleService {
    private final MotorcycleRepository motorcycleRepository;

    public MotorcycleService(MotorcycleRepository motorcycleRepository) {
        this.motorcycleRepository = motorcycleRepository;
    }

    @Transactional
    public Motorcycle createMotorcycle(Motorcycle motorcycle) {
        if (motorcycle.getQrCode() == null || motorcycle.getQrCode().isBlank()) {
            throw new ValidationException("QR code is required");
        }
        if (motorcycleRepository.existsByQrCode(motorcycle.getQrCode())) {
            throw new ValidationException("QR code already exists");
        }
        motorcycle.setMotorcycleId(UUID.randomUUID().toString());
        return motorcycleRepository.save(motorcycle);
    }

    @Transactional(readOnly = true)
    public Motorcycle getMotorcycleById(String motorcycleId) {
        return motorcycleRepository.findById(motorcycleId)
                .orElseThrow(() -> new ResourceNotFoundException("Motorcycle", "motorcycleId", motorcycleId));
    }

    @Transactional(readOnly = true)
    public Motorcycle getMotorcycleByQrCode(String qrCode) {
        return motorcycleRepository.findByQrCode(qrCode)
                .orElseThrow(() -> new ResourceNotFoundException("Motorcycle", "qrCode", qrCode));
    }

    @Transactional
    public Motorcycle updateMotorcycleStatus(String motorcycleId, Motorcycle.MotorcycleStatus newStatus) {
        Motorcycle motorcycle = getMotorcycleById(motorcycleId);
        motorcycle.setStatus(newStatus);
        return motorcycleRepository.save(motorcycle);
    }

    @Transactional(readOnly = true)
    public Page<Motorcycle> getMotorcyclesNeedingService(LocalDateTime date, Pageable pageable) {
        return motorcycleRepository.findMotorcyclesNeedingService(date, pageable);
    }
}
