package com.bunsen.api.aftercare.service;

import com.bunsen.api.aftercare.dto.MotorcycleDTO.*;
import com.bunsen.api.aftercare.enums.ETaskStatus;
import com.bunsen.api.aftercare.exception.DuplicateResourceException;
import com.bunsen.api.aftercare.exception.ResourceNotFoundException;
import com.bunsen.api.aftercare.exception.ValidationException;
import com.bunsen.api.aftercare.model.Motorcycle;
import com.bunsen.api.aftercare.model.ServiceTask;
import com.bunsen.api.aftercare.model.embedded.OwnerInfo;
import com.bunsen.api.aftercare.repository.MotorcycleRepository;
import com.bunsen.api.aftercare.repository.ServiceTaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final ActivityLogService activityLogService; // Inject ActivityLogService

    public MotorcycleService(MotorcycleRepository motorcycleRepository,
                             ServiceTaskRepository serviceTaskRepository,
                             ActivityLogService activityLogService) {
        this.motorcycleRepository = motorcycleRepository;
        this.serviceTaskRepository = serviceTaskRepository;
        this.activityLogService = activityLogService;
    }

    @Transactional(readOnly = true)
    public Page<Motorcycle> getMotorcyclesNeedingService(LocalDateTime date, Pageable pageable) {
        return motorcycleRepository.findMotorcyclesNeedingService(date, pageable);
    }

    @Transactional
    public MotorcycleResponse createMotorcycle(MotorcycleRequest request, String creatorId) {
        logger.info("Creating new motorcycle with QR code: {}", request.getQrCode());

        // Use DuplicateResourceException
        if (motorcycleRepository.existsByQrCode(request.getQrCode())) {
            throw new DuplicateResourceException("Motorcycle", "qrCode", request.getQrCode());
        }

        Motorcycle motorcycle = new Motorcycle();
        motorcycle.setId(UUID.randomUUID().toString());
        motorcycle.setQrCode(request.getQrCode());
        motorcycle.setModel(request.getModel());
        motorcycle.setPlateNumber(request.getPlateNumber());
        OwnerInfo ownerInfo = new OwnerInfo();
        ownerInfo.setName(request.getOwnerName());
        ownerInfo.setPhone(request.getOwnerPhone());
        ownerInfo.setEmail(request.getOwnerEmail());
        motorcycle.setOwner(ownerInfo);
        motorcycle.setLastServiceDate(request.getLastServiceDate());
        motorcycle.setStatus(Motorcycle.MotorcycleStatus.ACTIVE);

        Motorcycle savedMotorcycle = motorcycleRepository.save(motorcycle);
        logger.info("Motorcycle created successfully with ID: {}", savedMotorcycle.getId());

        activityLogService.createLog(creatorId, "MOTORCYCLE_REGISTERED",
                String.format("New motorcycle %s registered with QR code %s.", savedMotorcycle.getPlateNumber(), savedMotorcycle.getQrCode()));

        return mapToResponse(savedMotorcycle);
    }

    @Transactional(readOnly = true)
    public MotorcycleResponse getMotorcycleById(String motorcycleId) {
        // Use ResourceNotFoundException with full details
        Motorcycle motorcycle = motorcycleRepository.findById(motorcycleId)
                .orElseThrow(() -> new ResourceNotFoundException("Motorcycle", "id", motorcycleId));
        return mapToResponse(motorcycle);
    }

    @Transactional(readOnly = true)
    public MotorcycleResponse getMotorcycleByQrCode(String qrCode) {
        // Use ResourceNotFoundException with full details
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
    public MotorcycleResponse updateMotorcycle(String motorcycleId, MotorcycleRequest request, String updaterId) {
        logger.info("Updating motorcycle: {}", motorcycleId);

        Motorcycle motorcycle = motorcycleRepository.findById(motorcycleId)
                .orElseThrow(() -> new ResourceNotFoundException("Motorcycle", "id", motorcycleId));

        if (!request.getQrCode().equals(motorcycle.getQrCode())) {
            // Use DuplicateResourceException
            if (motorcycleRepository.existsByQrCode(request.getQrCode())) {
                throw new DuplicateResourceException("Motorcycle", "qrCode", request.getQrCode());
            }
            motorcycle.setQrCode(request.getQrCode());
        }

        motorcycle.setModel(request.getModel());
        motorcycle.setPlateNumber(request.getPlateNumber());
        OwnerInfo ownerInfo = new OwnerInfo();
        ownerInfo.setName(request.getOwnerName());
        ownerInfo.setPhone(request.getOwnerPhone());
        ownerInfo.setEmail(request.getOwnerEmail());
        motorcycle.setOwner(ownerInfo);
        motorcycle.setLastServiceDate(request.getLastServiceDate());

        Motorcycle updatedMotorcycle = motorcycleRepository.save(motorcycle);
        logger.info("Motorcycle updated successfully: {}", motorcycleId);

        activityLogService.createLog(updaterId, "MOTORCYCLE_UPDATED",
                String.format("Motorcycle %s details updated.", updatedMotorcycle.getPlateNumber()));

        return mapToResponse(updatedMotorcycle);
    }

    @Transactional
    public MotorcycleResponse updateMotorcycleStatus(String motorcycleId,
                                                     MotorcycleStatusUpdateRequest request, String updaterId) {
        logger.info("Updating motorcycle status for: {} to {}", motorcycleId, request.getStatus());

        Motorcycle motorcycle = motorcycleRepository.findById(motorcycleId)
                .orElseThrow(() -> new ResourceNotFoundException("Motorcycle", "id", motorcycleId));

        Motorcycle.MotorcycleStatus oldStatus = motorcycle.getStatus();
        motorcycle.setStatus(request.getStatus());

        Motorcycle updatedMotorcycle = motorcycleRepository.save(motorcycle);
        logger.info("Motorcycle status updated successfully: {} -> {}",
                motorcycleId, request.getStatus());

        activityLogService.createLog(updaterId, "MOTORCYCLE_STATUS_CHANGE",
                String.format("Motorcycle %s status changed from %s to %s.",
                        motorcycleId, oldStatus, updatedMotorcycle.getStatus()));

        return mapToResponse(updatedMotorcycle);
    }

    @Transactional
    public void deleteMotorcycle(String motorcycleId, String deleterId) {
        logger.info("Deleting motorcycle: {}", motorcycleId);

        Motorcycle motorcycle = motorcycleRepository.findById(motorcycleId)
                .orElseThrow(() -> new ResourceNotFoundException("Motorcycle", "id", motorcycleId));

        List<ServiceTask> activeTasks = serviceTaskRepository.findByMotorcycleId(motorcycleId)
                .stream()
                .filter(task -> task.getStatus() != ETaskStatus.COMPLETED)
                .toList();

        if (!activeTasks.isEmpty()) {
            // Use ValidationException for business rule violation
            throw new ValidationException("Cannot delete motorcycle with active service tasks");
        }

        motorcycleRepository.delete(motorcycle);
        logger.info("Motorcycle deleted successfully: {}", motorcycleId);

        activityLogService.createLog(deleterId, "MOTORCYCLE_DELETED",
                String.format("Motorcycle %s deleted.", motorcycleId));
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

        LocalDateTime cutoffDate = LocalDateTime.now().minusMonths(SERVICE_INTERVAL_MONTHS);
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
        List<ServiceTask> activeTasks = serviceTaskRepository.findByMotorcycleId(motorcycle.getId())
                .stream()
                .filter(task -> task.getStatus() != ETaskStatus.COMPLETED)
                .toList();

        boolean needsService;
        if (motorcycle.getLastServiceDate() == null) {
            needsService = true;
        } else {
            LocalDateTime cutoffDate = LocalDateTime.now().minusMonths(SERVICE_INTERVAL_MONTHS);
            needsService = motorcycle.getLastServiceDate().isBefore(cutoffDate);
        }

        return MotorcycleResponse.builder()
                .id(motorcycle.getId())
                .qrCode(motorcycle.getQrCode())
                .model(motorcycle.getModel())
                .plateNumber(motorcycle.getPlateNumber())
                .ownerName(motorcycle.getOwner().getName())
                .ownerPhone(motorcycle.getOwner().getPhone())
                .ownerEmail(motorcycle.getOwner().getEmail())
                .status(motorcycle.getStatus())
                .lastServiceDate(motorcycle.getLastServiceDate())
                .createdAt(motorcycle.getCreatedAt())
                .updatedAt(motorcycle.getUpdatedAt())
                .activeTasksCount(activeTasks.size())
                .needsService(needsService)
                .build();
    }
}
