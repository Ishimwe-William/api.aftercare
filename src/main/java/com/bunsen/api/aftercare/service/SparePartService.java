package com.bunsen.api.aftercare.service;

import com.bunsen.api.aftercare.dto.SparePartDTO.*;
import com.bunsen.api.aftercare.exception.LowStockException;
import com.bunsen.api.aftercare.exception.ResourceNotFoundException;
import com.bunsen.api.aftercare.exception.ValidationException;
import com.bunsen.api.aftercare.exception.DuplicateResourceException;
import com.bunsen.api.aftercare.model.ServiceTask;
import com.bunsen.api.aftercare.model.SparePart;
import com.bunsen.api.aftercare.model.TaskPartUsage;
import com.bunsen.api.aftercare.model.embedded.SupplierInfo;
import com.bunsen.api.aftercare.repository.ServiceTaskRepository;
import com.bunsen.api.aftercare.repository.SparePartRepository;
import com.bunsen.api.aftercare.repository.TaskPartUsageRepository;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SparePartService {
    private final SparePartRepository sparePartRepository;
    private final TaskPartUsageRepository taskPartUsageRepository;
    private final ServiceTaskRepository serviceTaskRepository;
    private final ActivityLogService activityLogService;

    private static final Logger log = LoggerFactory.getLogger(SparePartService.class);

    public SparePartService(SparePartRepository sparePartRepository,
                            TaskPartUsageRepository taskPartUsageRepository,
                            ServiceTaskRepository serviceTaskRepository,
                            ActivityLogService activityLogService) {
        this.sparePartRepository = sparePartRepository;
        this.taskPartUsageRepository = taskPartUsageRepository;
        this.serviceTaskRepository = serviceTaskRepository;
        this.activityLogService = activityLogService;
    }

    @Transactional(readOnly = true)
    public Page<SparePartResponse> getAllParts(Pageable pageable) {
        return sparePartRepository.findAll(pageable).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public SparePartResponse getPartById(String id) {
        SparePart part = sparePartRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SparePart", "id", id));
        return mapToResponse(part);
    }

    @Transactional(readOnly = true)
    public Page<SparePartResponse> searchByName(String keyword, Pageable pageable) {
        return sparePartRepository.searchByName(keyword, pageable).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<SparePartResponse> getLowStockParts(Pageable pageable) {
        return sparePartRepository.findLowStockParts(pageable).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<SparePartResponse> getOutOfStockParts(Pageable pageable) {
        return sparePartRepository.findOutOfStockParts(pageable).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public StockAlertResponse getStockAlerts() {
        List<SparePart> lowStock = sparePartRepository.findLowStockParts(Pageable.unpaged()).getContent();
        List<SparePart> outOfStock = sparePartRepository.findOutOfStockParts(Pageable.unpaged()).getContent();

        return StockAlertResponse.builder()
                .lowStockCount(lowStock.size())
                .outOfStockCount(outOfStock.size())
                .lowStockParts(lowStock.stream().map(this::mapToResponse).collect(Collectors.toList()))
                .outOfStockParts(outOfStock.stream().map(this::mapToResponse).collect(Collectors.toList()))
                .alertTimestamp(LocalDateTime.now())
                .build();
    }

    @Transactional(readOnly = true)
    public List<SparePartResponse> getPartsBySupplier(String supplierName) {
        return sparePartRepository.findBySupplierName(supplierName).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public SparePartResponse createPart(SparePartRequest request, String creatorId) {
        sparePartRepository.findByName(request.getName()).ifPresent(part -> {
            throw new DuplicateResourceException("SparePart", "name", request.getName());
        });

        if (request.getName() == null || request.getName().isBlank()) {
            throw new ValidationException("Part name is required");
        }
        if (request.getCost() == null || request.getCost().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Valid cost is required");
        }

        SparePart part = new SparePart();
        part.setId(UUID.randomUUID().toString());
        part.setName(request.getName());
        part.setDescription(request.getDescription());
        part.setQuantityAvailable(request.getQuantityAvailable() != null ? request.getQuantityAvailable() : 0);
        part.setCost(request.getCost());

        SupplierInfo supplierInfo = new SupplierInfo();
        supplierInfo.setName(request.getSupplierName());
        supplierInfo.setContact(request.getSupplierContact());
        part.setSupplier(supplierInfo);

        part.setLowStockThreshold(request.getLowStockThreshold() != null ? request.getLowStockThreshold() : 10.0);

        SparePart saved = sparePartRepository.save(part);
        log.info("Created spare part: {}", saved.getName());

        activityLogService.createLog(creatorId, "PART_CREATED",
                String.format("New spare part %s created with ID %s.", saved.getName(), saved.getId()));

        return mapToResponse(saved);
    }

    @Transactional
    public SparePartResponse updatePart(String id, SparePartRequest request) {
        SparePart part = sparePartRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SparePart", "id", id));

        part.setName(request.getName());
        part.setDescription(request.getDescription());
        part.setQuantityAvailable(request.getQuantityAvailable());
        part.setCost(request.getCost());

        SupplierInfo supplierInfo = new SupplierInfo();
        supplierInfo.setName(request.getSupplierName());
        supplierInfo.setContact(request.getSupplierContact());
        part.setSupplier(supplierInfo);

        part.setLowStockThreshold(request.getLowStockThreshold());

        SparePart updated = sparePartRepository.save(part);
        log.info("Updated spare part: {}", updated.getName());
        return mapToResponse(updated);
    }

    @Transactional
    public SparePartResponse adjustStock(String id, StockAdjustmentRequest request, String updaterId) {
        SparePart part = sparePartRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SparePart", "id", id));

        double oldQuantity = part.getQuantityAvailable();
        double newQuantity = request.getTargetQuantity();
        double quantityChange = newQuantity - oldQuantity;

        if (newQuantity < 0) {
            throw new ValidationException("Stock quantity cannot be negative");
        }

        String logAction;
        String logMessageAction;

        if (quantityChange > 0.0) {
            logAction = "STOCK_ADDED";
            logMessageAction = "added";
        } else if (quantityChange < 0.0) {
            logAction = "STOCK_SUBTRACTED";
            logMessageAction = "removed";
        } else {
            if (request.getReason() == null || request.getReason().trim().isEmpty()) {
                throw new ValidationException("Target quantity is the same as current stock. Provide a reason or change the target quantity.");
            }
            logAction = "STOCK_NO_CHANGE";
            logMessageAction = "kept the same";
        }

        part.setQuantityAvailable(newQuantity);
        SparePart updated = sparePartRepository.save(part);

        String activityMessage = String.format("Stock for part %s (%s) adjusted. %.2f units were %s. Quantity is now %.2f (Reason: %s).",
                part.getName(), part.getId(), Math.abs(quantityChange), logMessageAction, newQuantity, request.getReason());

        activityLogService.createLog(updaterId, logAction, activityMessage);

        return mapToResponse(updated);
    }

    @Transactional
    public void deletePart(String id, String deleterId) {
        SparePart part = sparePartRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SparePart", "id", id));

        List<TaskPartUsage> usages = taskPartUsageRepository.findByPartId(id);
        if (!usages.isEmpty()) {
            throw new ValidationException("Cannot delete part with existing usage records");
        }

        sparePartRepository.delete(part);
        log.info("Deleted spare part: {}", part.getName());

        activityLogService.createLog(deleterId, "PART_DELETED",
                String.format("Spare part %s (%s) deleted.", part.getName(), part.getId()));
    }

    @Transactional
    public PartUsageResponse logPartUsage(PartUsageRequest request) {
        SparePart part = sparePartRepository.findById(request.getPartId())
                .orElseThrow(() -> new ResourceNotFoundException("SparePart", "id", request.getPartId()));

        ServiceTask task = serviceTaskRepository.findById(request.getTaskId())
                .orElseThrow(() -> new ResourceNotFoundException("ServiceTask", "id", request.getTaskId()));

        if (part.getQuantityAvailable() < request.getQuantityUsed()) {
            throw new LowStockException(part.getName(), part.getQuantityAvailable(), request.getQuantityUsed());
        }

        part.setQuantityAvailable(part.getQuantityAvailable() - request.getQuantityUsed());
        sparePartRepository.save(part);

        TaskPartUsage usage = new TaskPartUsage();
        usage.setUsageId(UUID.randomUUID().toString());
        usage.setTask(task);
        usage.setPart(part);
        usage.setQuantityUsed(request.getQuantityUsed());
        usage.setNotes(request.getNotes());

        TaskPartUsage saved = taskPartUsageRepository.save(usage);
        log.info("Logged usage: {} x {} for task {}", request.getQuantityUsed(),
                part.getName(), task.getId());

        activityLogService.createLog(task.getTechnician().getId(), "PART_USED",
                String.format("Used %.2f of part %s (%s) for task %s.",
                        request.getQuantityUsed(), part.getName(), part.getId(), task.getId()));

        return mapUsageToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<PartUsageResponse> getPartUsageHistory(String partId) {
        return taskPartUsageRepository.findByPartId(partId).stream()
                .map(this::mapUsageToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PartUsageResponse> getTaskPartUsages(String taskId) {
        return taskPartUsageRepository.findByTaskId(taskId).stream()
                .map(this::mapUsageToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMostUsedParts() {
        List<Object[]> results = taskPartUsageRepository.findMostUsedParts();
        return results.stream().map(result -> {
            Map<String, Object> map = new HashMap<>();
            map.put("partName", result[0]);
            map.put("totalUsed", result[1]);
            return map;
        }).collect(Collectors.toList());
    }

    public byte[] exportInventoryReport() {
        List<SparePart> parts = sparePartRepository.findAll();

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             PrintWriter writer = new PrintWriter(baos)) {

            writer.println("Part ID,Name,Description,Quantity,Cost,Supplier,Supplier Contact,Low Stock Threshold,Status,Total Used");

            for (SparePart part : parts) {
                String status = part.getQuantityAvailable() == 0 ? "OUT_OF_STOCK" :
                        part.getQuantityAvailable() <= part.getLowStockThreshold() ? "LOW_STOCK" : "NORMAL";

                Long totalUsed = taskPartUsageRepository.getTotalQuantityUsedForPart(part.getId());

                writer.printf("%s,%s,%s,%.2f,%s,%s,%s,%.2f,%s,%d%n",
                        part.getId(),
                        escapeCSV(part.getName()),
                        escapeCSV(part.getDescription()),
                        part.getQuantityAvailable(),
                        part.getCost(),
                        escapeCSV(part.getSupplier().getName()),
                        escapeCSV(part.getSupplier().getContact()),
                        part.getLowStockThreshold(),
                        status,
                        totalUsed != null ? totalUsed : 0);
            }

            writer.flush();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate report", e);
        }
    }

    private String escapeCSV(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private SparePartResponse mapToResponse(SparePart part) {
        Long totalUsed = taskPartUsageRepository.getTotalQuantityUsedForPart(part.getId());

        return SparePartResponse.builder()
                .id(part.getId())
                .name(part.getName())
                .description(part.getDescription())
                .quantityAvailable(part.getQuantityAvailable())
                .cost(part.getCost())
                .supplierName(part.getSupplier().getName())
                .supplierContact(part.getSupplier().getContact())
                .lowStockThreshold(part.getLowStockThreshold())
                .isLowStock(part.getQuantityAvailable() <= part.getLowStockThreshold())
                .isOutOfStock(part.getQuantityAvailable() == 0)
                .totalUsed(totalUsed != null ? totalUsed : 0L)
                .createdAt(part.getCreatedAt())
                .updatedAt(part.getUpdatedAt())
                .build();
    }

    private PartUsageResponse mapUsageToResponse(TaskPartUsage usage) {
        return PartUsageResponse.builder()
                .usageId(usage.getUsageId())
                .taskId(usage.getTask().getId())
                .partId(usage.getPart().getId())
                .partName(usage.getPart().getName())
                .quantityUsed(usage.getQuantityUsed())
                .cost(usage.getPart().getCost())
                .notes(usage.getNotes())
                .usedAt(usage.getUsedAt())
                .build();
    }

    @Transactional
    public PartUsageResponse updatePartUsage(String usageId, @Valid PartUsageRequest request) {
        TaskPartUsage usage = taskPartUsageRepository.findByUsageId(usageId)
                .orElseThrow(() -> new ResourceNotFoundException("Part usage not found with id: " + usageId));

        SparePart part = sparePartRepository.findById(request.getPartId())
                .orElseThrow(() -> new ResourceNotFoundException("SparePart", "id", request.getPartId()));

        double oldQuantity = usage.getQuantityUsed();
        double newQuantity = request.getQuantityUsed();
        double quantityDifference = newQuantity - oldQuantity;

        if (quantityDifference > 0) {
            if (part.getQuantityAvailable() < quantityDifference) {
                throw new LowStockException(part.getName(), part.getQuantityAvailable(), quantityDifference);
            }
            part.setQuantityAvailable(part.getQuantityAvailable() - quantityDifference);
        } else if (quantityDifference < 0) {
            part.setQuantityAvailable(part.getQuantityAvailable() + Math.abs(quantityDifference));
        }

        sparePartRepository.save(part);

        usage.setQuantityUsed(newQuantity);
        usage.setNotes(request.getNotes());
        TaskPartUsage updated = taskPartUsageRepository.save(usage);

        log.info("Updated part usage: {} x {} for task {}", newQuantity, part.getName(), usage.getTask().getId());

        activityLogService.createLog(usage.getTask().getTechnician().getId(), "PART_USAGE_UPDATED",
                String.format("Updated usage of part %s for task %s. Old quantity: %.2f, New quantity: %.2f",
                        part.getName(), usage.getTask().getId(), oldQuantity, newQuantity));

        return mapUsageToResponse(updated);
    }

    @Transactional
    public void deletePartUsage(String usageId) {
        TaskPartUsage usage = taskPartUsageRepository.findByUsageId(usageId)
                .orElseThrow(() -> new ResourceNotFoundException("Part usage not found with id: " + usageId));

        SparePart part = usage.getPart();

        part.setQuantityAvailable(part.getQuantityAvailable() + usage.getQuantityUsed());
        sparePartRepository.save(part);

        String taskId = usage.getTask().getId();
        String technicianId = usage.getTask().getTechnician().getId();
        double quantityUsed = usage.getQuantityUsed();
        String partName = part.getName();

        taskPartUsageRepository.delete(usage);

        log.info("Deleted part usage: {} x {} for task {}", quantityUsed, partName, taskId);

        activityLogService.createLog(technicianId, "PART_USAGE_DELETED",
                String.format("Deleted usage of %.2f units of part %s for task %s. Stock restored.",
                        quantityUsed, partName, taskId));
    }

    @Transactional(readOnly = true)
    public List<SparePartResponse> checkSimilarParts(String name) {
        return sparePartRepository.searchByName(name, PageRequest.of(0, 5)).getContent()
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SupplierSuggestion> checkSimilarSuppliers(String supplierName) {
        return sparePartRepository.findSimilarSuppliers(supplierName).stream()
                .map(arr -> SupplierSuggestion.builder()
                        .name((String) arr[0])
                        .contact((String) arr[1])
                        .build())
                .collect(Collectors.toList());
    }
}