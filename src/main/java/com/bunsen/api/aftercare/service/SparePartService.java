package com.bunsen.api.aftercare.service;

import com.bunsen.api.aftercare.dto.request.PartUsageRequest;
import com.bunsen.api.aftercare.dto.request.SparePartRequest;
import com.bunsen.api.aftercare.dto.request.StockAdjustmentRequest;
import com.bunsen.api.aftercare.dto.response.PartUsageResponse;
import com.bunsen.api.aftercare.dto.response.SparePartResponse;
import com.bunsen.api.aftercare.dto.response.StockAlertResponse;
import com.bunsen.api.aftercare.exception.LowStockException; // Use specific exception
import com.bunsen.api.aftercare.exception.ResourceNotFoundException;
import com.bunsen.api.aftercare.exception.ValidationException;
import com.bunsen.api.aftercare.exception.DuplicateResourceException; // Use specific exception
import com.bunsen.api.aftercare.model.ServiceTask;
import com.bunsen.api.aftercare.model.SparePart;
import com.bunsen.api.aftercare.model.TaskPartUsage;
import com.bunsen.api.aftercare.model.embedded.SupplierInfo;
import com.bunsen.api.aftercare.repository.ServiceTaskRepository;
import com.bunsen.api.aftercare.repository.SparePartRepository;
import com.bunsen.api.aftercare.repository.TaskPartUsageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SparePartService {
    private final SparePartRepository sparePartRepository;
    private final TaskPartUsageRepository taskPartUsageRepository;
    private final ServiceTaskRepository serviceTaskRepository;
    private final ActivityLogService activityLogService; // Inject ActivityLogService

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

    @Transactional
    public SparePart createSparePart(SparePart part) {
        // Redundant as there is a createPart method, but updating for consistency
        if (part.getName() == null || part.getName().isBlank()) {
            throw new ValidationException("Part name is required");
        }
        if (part.getCost() == null || part.getCost().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Valid cost is required");
        }

        sparePartRepository.findByName(part.getName()).ifPresent(p -> {
            throw new DuplicateResourceException("SparePart", "name", part.getName());
        });

        part.setId(UUID.randomUUID().toString());
        return sparePartRepository.save(part);
    }

    @Transactional(readOnly = true)
    public SparePart getSparePartById(String partId) {
        return sparePartRepository.findById(partId)
                // Use ResourceNotFoundException with full details
                .orElseThrow(() -> new ResourceNotFoundException("SparePart", "partId", partId));
    }

    @Transactional
    public SparePart updateStock(String partId, int quantityChange) {
        SparePart part = getSparePartById(partId);
        int newQuantity = part.getQuantityAvailable() + quantityChange;

        // Use LowStockException
        if (newQuantity < 0) {
            throw new LowStockException(part.getName(), part.getQuantityAvailable(), -quantityChange);
        }

        int oldQuantity = part.getQuantityAvailable();
        part.setQuantityAvailable(newQuantity);

        SparePart updatedPart = sparePartRepository.save(part);
        activityLogService.createLog("SYSTEM", "STOCK_ADJUSTMENT",
                String.format("Stock for part %s (%s) adjusted: %d -> %d. Change: %d.",
                        part.getName(), part.getId(), oldQuantity, newQuantity, quantityChange));

        return updatedPart;
    }

    @Transactional(readOnly = true)
    public List<SparePart> searchPartsByName(String keyword) {
        return sparePartRepository.searchByName(keyword, Pageable.unpaged()).getContent();
    }

    public Page<SparePartResponse> getAllParts(Pageable pageable) {
        return sparePartRepository.findAll(pageable).map(this::mapToResponse);
    }

    public SparePartResponse getPartById(String id) {
        SparePart part = sparePartRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Spare part not found with id: " + id));
        return mapToResponse(part);
    }

    public Page<SparePartResponse> searchByName(String keyword, Pageable pageable) {
        return sparePartRepository.searchByName(keyword, pageable).map(this::mapToResponse);
    }

    public Page<SparePartResponse> getLowStockParts(Pageable pageable) {
        return sparePartRepository.findLowStockParts(pageable).map(this::mapToResponse);
    }

    public Page<SparePartResponse> getOutOfStockParts(Pageable pageable) {
        return sparePartRepository.findOutOfStockParts(pageable).map(this::mapToResponse);
    }

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

    public List<SparePartResponse> getPartsBySupplier(String supplierName) {
        return sparePartRepository.findBySupplierName(supplierName).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public SparePartResponse createPart(SparePartRequest request) {
        // Use DuplicateResourceException
        sparePartRepository.findByName(request.getName()).ifPresent(part -> {
            throw new DuplicateResourceException("SparePart", "name", request.getName());
        });

        // Basic validation check (DRY from createSparePart if possible, but keeping here for clarity)
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

        part.setLowStockThreshold(request.getLowStockThreshold() != null ? request.getLowStockThreshold() : 10);

        SparePart saved = sparePartRepository.save(part);
        log.info("Created spare part: {}", saved.getName());

        activityLogService.createLog("SYSTEM", "PART_CREATED",
                String.format("New spare part %s created with ID %s.", saved.getName(), saved.getId()));

        return mapToResponse(saved);
    }

    @Transactional
    public SparePartResponse updatePart(String id, SparePartRequest request) {
        SparePart part = sparePartRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Spare part not found with id: " + id));

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
    public SparePartResponse adjustStock(String id, StockAdjustmentRequest request) {
        SparePart part = sparePartRepository.findById(id)
                // Use ResourceNotFoundException
                .orElseThrow(() -> new ResourceNotFoundException("SparePart", "id", id));

        int newQuantity;
        int oldQuantity = part.getQuantityAvailable();
        String logAction;

        if ("ADD".equalsIgnoreCase(request.getAdjustmentType())) {
            newQuantity = part.getQuantityAvailable() + request.getQuantity();
            logAction = "STOCK_ADDED";
        } else if ("SUBTRACT".equalsIgnoreCase(request.getAdjustmentType())) {
            newQuantity = part.getQuantityAvailable() - request.getQuantity();
            // Use LowStockException
            if (newQuantity < 0) {
                throw new LowStockException(part.getName(), part.getQuantityAvailable(), request.getQuantity());
            }
            logAction = "STOCK_SUBTRACTED";
        } else {
            // Use ValidationException
            throw new ValidationException("Invalid adjustment type. Use ADD or SUBTRACT");
        }

        part.setQuantityAvailable(newQuantity);
        SparePart updated = sparePartRepository.save(part);
        log.info("Adjusted stock for {}: {} -> {}", part.getName(),
                oldQuantity, newQuantity);

        activityLogService.createLog("SYSTEM", logAction,
                String.format("Stock for part %s (%s) adjusted. Quantity changed by %d to %d.",
                        part.getName(), part.getId(), request.getQuantity(), newQuantity));

        return mapToResponse(updated);
    }

    @Transactional
    public void deletePart(String id) {
        SparePart part = sparePartRepository.findById(id)
                // Use ResourceNotFoundException
                .orElseThrow(() -> new ResourceNotFoundException("SparePart", "id", id));

        List<TaskPartUsage> usages = taskPartUsageRepository.findByPartId(id);
        if (!usages.isEmpty()) {
            // Use ValidationException (or a specific custom exception for constraints)
            throw new ValidationException("Cannot delete part with existing usage records");
        }

        sparePartRepository.delete(part);
        log.info("Deleted spare part: {}", part.getName());

        activityLogService.createLog("SYSTEM", "PART_DELETED",
                String.format("Spare part %s (%s) deleted.", part.getName(), part.getId()));
    }

    @Transactional
    public PartUsageResponse logPartUsage(PartUsageRequest request) {
        SparePart part = sparePartRepository.findById(request.getPartId())
                // Use ResourceNotFoundException
                .orElseThrow(() -> new ResourceNotFoundException("SparePart", "id", request.getPartId()));

        ServiceTask task = serviceTaskRepository.findById(request.getTaskId())
                // Use ResourceNotFoundException
                .orElseThrow(() -> new ResourceNotFoundException("ServiceTask", "id", request.getTaskId()));

        // Use LowStockException
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
                String.format("Used %d of part %s (%s) for task %s.",
                        request.getQuantityUsed(), part.getName(), part.getId(), task.getId()));

        return mapUsageToResponse(saved);
    }

    public List<PartUsageResponse> getPartUsageHistory(String partId) {
        return taskPartUsageRepository.findByPartId(partId).stream()
                .map(this::mapUsageToResponse)
                .collect(Collectors.toList());
    }

    public List<PartUsageResponse> getTaskPartUsages(String taskId) {
        return taskPartUsageRepository.findByTaskId(taskId).stream()
                .map(this::mapUsageToResponse)
                .collect(Collectors.toList());
    }

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

            writer.println("Part ID,Name,Description,Quantity,Cost,Supplier,Low Stock Threshold,Status");

            for (SparePart part : parts) {
                String status = part.getQuantityAvailable() == 0 ? "OUT_OF_STOCK" :
                        part.getQuantityAvailable() <= part.getLowStockThreshold() ? "LOW_STOCK" : "NORMAL";

                writer.printf("%s,%s,%s,%d,%s,%s,%d,%s%n",
                        part.getId(),
                        escapeCSV(part.getName()),
                        escapeCSV(part.getDescription()),
                        part.getQuantityAvailable(),
                        part.getCost(),
                        escapeCSV(part.getSupplier().getName()),
                        part.getLowStockThreshold(),
                        status);
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
                .partId(part.getId())
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
                .notes(usage.getNotes())
                .usedAt(usage.getUsedAt())
                .build();
    }
}
