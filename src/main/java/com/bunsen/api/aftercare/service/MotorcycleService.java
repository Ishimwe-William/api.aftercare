
package com.bunsen.api.aftercare.service;

import com.bunsen.api.aftercare.dto.request.MotorcycleRequest;
import com.bunsen.api.aftercare.dto.request.MotorcycleStatusUpdateRequest;
import com.bunsen.api.aftercare.dto.response.MotorcycleResponse;
import com.bunsen.api.aftercare.dto.response.MotorcycleStatisticsResponse;
import com.bunsen.api.aftercare.exception.DuplicateResourceException;
import com.bunsen.api.aftercare.exception.ResourceNotFoundException;
import com.bunsen.api.aftercare.exception.ValidationException;
import com.bunsen.api.aftercare.model.Motorcycle;
import com.bunsen.api.aftercare.model.ServiceTask;
import com.bunsen.api.aftercare.repository.MotorcycleRepository;
import com.bunsen.api.aftercare.repository.ServiceTaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class MotorcycleService {
    private static final Logger logger = LoggerFactory.getLogger(MotorcycleService.class);
    private static final int SERVICE_INTERVAL_MONTHS = 6;

    private final MotorcycleRepository motorcycleRepository;
    private final ServiceTaskRepository serviceTaskRepository;

    public MotorcycleService(MotorcycleRepository motorcycleRepository,
                             ServiceTaskRepository serviceTaskRepository) {
        this.motorcycleRepository = motorcycleRepository;
        this.serviceTaskRepository = serviceTaskRepository;
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
    public Page<Motorcycle> getMotorcyclesNeedingService(LocalDateTime date, Pageable pageable) {
        return motorcycleRepository.findMotorcyclesNeedingService(date, pageable);
    }

    @Transactional
    public MotorcycleResponse createMotorcycle(MotorcycleRequest request) {
        logger.info("Creating new motorcycle with QR code: {}", request.getQrCode());

        if (motorcycleRepository.existsByQrCode(request.getQrCode())) {
            throw new DuplicateResourceException("Motorcycle", "qrCode", request.getQrCode());
        }

        Motorcycle motorcycle = new Motorcycle();
        motorcycle.setMotorcycleId(UUID.randomUUID().toString());
        motorcycle.setQrCode(request.getQrCode());
        motorcycle.setModel(request.getModel());
        motorcycle.setPlateNumber(request.getPlateNumber());
        motorcycle.setOwnerName(request.getOwnerName());
        motorcycle.setOwnerPhone(request.getOwnerPhone());
        motorcycle.setOwnerEmail(request.getOwnerEmail());
        motorcycle.setLastServiceDate(request.getLastServiceDate());
        motorcycle.setStatus(Motorcycle.MotorcycleStatus.ACTIVE);

        Motorcycle savedMotorcycle = motorcycleRepository.save(motorcycle);
        logger.info("Motorcycle created successfully with ID: {}", savedMotorcycle.getMotorcycleId());

        return mapToResponse(savedMotorcycle);
    }

    @Transactional(readOnly = true)
    public MotorcycleResponse getMotorcycleById(String motorcycleId) {
        Motorcycle motorcycle = motorcycleRepository.findById(motorcycleId)
                .orElseThrow(() -> new ResourceNotFoundException("Motorcycle", "motorcycleId", motorcycleId));
        return mapToResponse(motorcycle);
    }

    @Transactional(readOnly = true)
    public MotorcycleResponse getMotorcycleByQrCode(String qrCode) {
        Motorcycle motorcycle = motorcycleRepository.findByQrCode(qrCode)
                .orElseThrow(() -> new ResourceNotFoundException("Motorcycle", "qrCode", qrCode));
        return mapToResponse(motorcycle);
    }

    @Transactional(readOnly = true)
    public Page<MotorcycleResponse> getAllMotorcycles(Pageable pageable) {
        return motorcycleRepository.findAll(pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public List<MotorcycleResponse> getMotorcyclesByStatus(Motorcycle.MotorcycleStatus status) {
        return motorcycleRepository.findByStatus(status).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MotorcycleResponse> getMotorcyclesByOwnerPhone(String ownerPhone) {
        return motorcycleRepository.findByOwnerPhone(ownerPhone).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MotorcycleResponse> getMotorcyclesByOwnerEmail(String ownerEmail) {
        return motorcycleRepository.findByOwnerEmail(ownerEmail).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<MotorcycleResponse> getMotorcyclesInService(Pageable pageable) {
        return motorcycleRepository.findAllInService(pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<MotorcycleResponse> getMotorcyclesNeedingService(Pageable pageable) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusMonths(SERVICE_INTERVAL_MONTHS);
        return motorcycleRepository.findMotorcyclesNeedingService(cutoffDate, pageable)
                .map(this::mapToResponse);
    }

    @Transactional
    public MotorcycleResponse updateMotorcycle(String motorcycleId, MotorcycleRequest request) {
        logger.info("Updating motorcycle: {}", motorcycleId);

        Motorcycle motorcycle = motorcycleRepository.findById(motorcycleId)
                .orElseThrow(() -> new ResourceNotFoundException("Motorcycle", "motorcycleId", motorcycleId));

        if (!request.getQrCode().equals(motorcycle.getQrCode())) {
            if (motorcycleRepository.existsByQrCode(request.getQrCode())) {
                throw new DuplicateResourceException("Motorcycle", "qrCode", request.getQrCode());
            }
            motorcycle.setQrCode(request.getQrCode());
        }

        motorcycle.setModel(request.getModel());
        motorcycle.setPlateNumber(request.getPlateNumber());
        motorcycle.setOwnerName(request.getOwnerName());
        motorcycle.setOwnerPhone(request.getOwnerPhone());
        motorcycle.setOwnerEmail(request.getOwnerEmail());
        motorcycle.setLastServiceDate(request.getLastServiceDate());

        Motorcycle updatedMotorcycle = motorcycleRepository.save(motorcycle);
        logger.info("Motorcycle updated successfully: {}", motorcycleId);

        return mapToResponse(updatedMotorcycle);
    }

    @Transactional
    public MotorcycleResponse updateMotorcycleStatus(String motorcycleId,
                                                     MotorcycleStatusUpdateRequest request) {
        logger.info("Updating motorcycle status for: {} to {}", motorcycleId, request.getStatus());

        Motorcycle motorcycle = motorcycleRepository.findById(motorcycleId)
                .orElseThrow(() -> new ResourceNotFoundException("Motorcycle", "motorcycleId", motorcycleId));

        motorcycle.setStatus(request.getStatus());

        Motorcycle updatedMotorcycle = motorcycleRepository.save(motorcycle);
        logger.info("Motorcycle status updated successfully: {} -> {}",
                motorcycleId, request.getStatus());

        return mapToResponse(updatedMotorcycle);
    }

    @Transactional
    public void deleteMotorcycle(String motorcycleId) {
        logger.info("Deleting motorcycle: {}", motorcycleId);

        Motorcycle motorcycle = motorcycleRepository.findById(motorcycleId)
                .orElseThrow(() -> new ResourceNotFoundException("Motorcycle", "motorcycleId", motorcycleId));

        List<ServiceTask> activeTasks = serviceTaskRepository.findByMotorcycleMotorcycleId(motorcycleId)
                .stream()
                .filter(task -> task.getStatus() != ServiceTask.TaskStatus.COMPLETED)
                .collect(Collectors.toList());

        if (!activeTasks.isEmpty()) {
            throw new ValidationException("Cannot delete motorcycle with active service tasks");
        }

        motorcycleRepository.delete(motorcycle);
        logger.info("Motorcycle deleted successfully: {}", motorcycleId);
    }

    @Transactional(readOnly = true)
    public MotorcycleStatisticsResponse getMotorcycleStatistics() {
        List<Motorcycle> allMotorcycles = motorcycleRepository.findAll();

        long totalMotorcycles = allMotorcycles.size();
        long activeMotorcycles = allMotorcycles.stream()
                .filter(m -> m.getStatus() == Motorcycle.MotorcycleStatus.ACTIVE)
                .count();
        long inactiveMotorcycles = allMotorcycles.stream()
                .filter(m -> m.getStatus() == Motorcycle.MotorcycleStatus.INACTIVE)
                .count();
        long inServiceMotorcycles = allMotorcycles.stream()
                .filter(m -> m.getStatus() == Motorcycle.MotorcycleStatus.IN_SERVICE)
                .count();

        LocalDate cutoffDate = LocalDate.now().minusMonths(SERVICE_INTERVAL_MONTHS);
        long motorcyclesNeedingService = allMotorcycles.stream()
                .filter(m -> m.getLastServiceDate() == null || m.getLastServiceDate().isBefore(cutoffDate))
                .count();

        return MotorcycleStatisticsResponse.builder()
                .totalMotorcycles(totalMotorcycles)
                .activeMotorcycles(activeMotorcycles)
                .inactiveMotorcycles(inactiveMotorcycles)
                .inServiceMotorcycles(inServiceMotorcycles)
                .motorcyclesNeedingService(motorcyclesNeedingService)
                .build();
    }

    private MotorcycleResponse mapToResponse(Motorcycle motorcycle) {
        List<ServiceTask> activeTasks = serviceTaskRepository.findByMotorcycleMotorcycleId(motorcycle.getMotorcycleId())
                .stream()
                .filter(task -> task.getStatus() != ServiceTask.TaskStatus.COMPLETED)
                .toList();

        boolean needsService = false;
        if (motorcycle.getLastServiceDate() == null) {
            needsService = true;
        } else {
            LocalDate cutoffDate = LocalDate.now().minusMonths(SERVICE_INTERVAL_MONTHS);
            needsService = motorcycle.getLastServiceDate().isBefore(cutoffDate);
        }

        return MotorcycleResponse.builder()
                .motorcycleId(motorcycle.getMotorcycleId())
                .qrCode(motorcycle.getQrCode())
                .model(motorcycle.getModel())
                .plateNumber(motorcycle.getPlateNumber())
                .ownerName(motorcycle.getOwnerName())
                .ownerPhone(motorcycle.getOwnerPhone())
                .ownerEmail(motorcycle.getOwnerEmail())
                .status(motorcycle.getStatus())
                .lastServiceDate(motorcycle.getLastServiceDate())
                .createdAt(motorcycle.getCreatedAt())
                .updatedAt(motorcycle.getUpdatedAt())
                .activeTasksCount(activeTasks.size())
                .needsService(needsService)
                .build();
    }
}
