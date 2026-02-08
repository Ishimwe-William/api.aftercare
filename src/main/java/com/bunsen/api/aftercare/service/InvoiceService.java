package com.bunsen.api.aftercare.service;

import com.bunsen.api.aftercare.enums.ETaskStatus;
import com.bunsen.api.aftercare.exception.ResourceNotFoundException;
import com.bunsen.api.aftercare.exception.TaskStatusException;
import com.bunsen.api.aftercare.model.*;
import com.bunsen.api.aftercare.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class InvoiceService {
    private final InvoiceRepository invoiceRepository;
    private final InvoiceLineItemRepository invoiceLineItemRepository;
    private final ServiceTaskRepository serviceTaskRepository;
    private final TaskPartUsageRepository taskPartUsageRepository;
    private final KnownIssueRepository knownIssueRepository;
    private final MotorcycleRepository motorcycleRepository;

    public InvoiceService(InvoiceRepository invoiceRepository,
                          InvoiceLineItemRepository invoiceLineItemRepository,
                          ServiceTaskRepository serviceTaskRepository,
                          TaskPartUsageRepository taskPartUsageRepository,
                          KnownIssueRepository knownIssueRepository,
                          MotorcycleRepository motorcycleRepository) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceLineItemRepository = invoiceLineItemRepository;
        this.serviceTaskRepository = serviceTaskRepository;
        this.taskPartUsageRepository = taskPartUsageRepository;
        this.knownIssueRepository = knownIssueRepository;
        this.motorcycleRepository = motorcycleRepository;
    }

    @Transactional
    public Invoice generateInvoice(String taskId, BigDecimal partsCost, BigDecimal discount, String notes, BigDecimal zero) {
        // Enforce the business rule: Task can have at most 1 invoice.
        Optional<Invoice> existingInvoice = invoiceRepository.findByTaskId(taskId);
        if (existingInvoice.isPresent()) {
            throw new TaskStatusException("An invoice already exists for this task. Use updateInvoice instead.", "generate invoice");
        }

        ServiceTask task = serviceTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceTask", "taskId", taskId));

        Motorcycle motorcycle = motorcycleRepository.findByPlateNumber(task.getMotorcycle().getPlateNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Motorcycle", "plateNumber", task.getMotorcycle().getPlateNumber()));

        if (task.getStatus() != ETaskStatus.COMPLETED) {
            throw new TaskStatusException(task.getStatus().name(), "generate invoice");
        }

        // Get the known issue price based on the issue type
        BigDecimal issueCost = BigDecimal.ZERO;
        String knownIssueName = task.getIssueType();

        if (knownIssueName != null && !knownIssueName.trim().isEmpty()) {
            Optional<KnownIssue> knownIssueOpt = knownIssueRepository.findByNameIgnoreCase(knownIssueName);
            if (knownIssueOpt.isPresent()) {
                issueCost = knownIssueOpt.get().getPrice();
            }
        }

        // Calculate total cost
        BigDecimal actualDiscount = discount != null ? discount : BigDecimal.ZERO;
        BigDecimal totalCost = issueCost.add(partsCost).subtract(actualDiscount);

        Invoice invoice = new Invoice();
        invoice.setInvoiceId(UUID.randomUUID().toString());
        invoice.setTask(task);

        // Store static snapshot of task/motorcycle/technician info
        invoice.setMotorcycleModel(task.getMotorcycle().getModel());
        invoice.setMotorcycleOwnerName(motorcycle.getOwner().getName());
        invoice.setMotorcycleOwnerPhone(motorcycle.getOwner().getPhone());
        invoice.setMotorcycleOwnerEmail(motorcycle.getOwner().getEmail());
        invoice.setMotorcyclePlateNumber(task.getMotorcycle().getPlateNumber());
        invoice.setTechnicianName(task.getTechnician().getFullName());
        invoice.setIssueType(task.getIssueType());

        // Store known issue details as snapshot
        invoice.setKnownIssueName(knownIssueName);
        invoice.setKnownIssuePrice(issueCost);

        invoice.setIssueCost(issueCost);
        invoice.setPartsCost(partsCost);
        invoice.setDiscount(actualDiscount);
        invoice.setTotalCost(totalCost);
        invoice.setGeneratedAt(LocalDateTime.now());
        invoice.setNotes(notes);

        Invoice savedInvoice = invoiceRepository.save(invoice);

        // Create line items for parts used
        List<TaskPartUsage> partUsages = taskPartUsageRepository.findByTaskId(taskId);
        for (TaskPartUsage usage : partUsages) {
            InvoiceLineItem lineItem = new InvoiceLineItem();
            lineItem.setLineItemId(UUID.randomUUID().toString());
            lineItem.setInvoice(savedInvoice);
            lineItem.setPartId(usage.getPart().getId());
            lineItem.setPartName(usage.getPart().getName());
            lineItem.setPartDescription(usage.getPart().getDescription());
            lineItem.setQuantityUsed(usage.getQuantityUsed());
            lineItem.setUnitCost(usage.getPart().getCost());
            lineItem.setTotalCost(usage.getPart().getCost().multiply(BigDecimal.valueOf(usage.getQuantityUsed())));
            lineItem.setNotes(usage.getNotes());

            invoiceLineItemRepository.save(lineItem);
        }

        return savedInvoice;
    }

    /**
     * Method to update an existing invoice.
     */
    @Transactional
    public Invoice updateInvoice(String invoiceId, BigDecimal partsCost, BigDecimal discount, String notes) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", "invoiceId", invoiceId));

        BigDecimal actualDiscount = discount != null ? discount : BigDecimal.ZERO;

        // Get the issue cost from the stored snapshot (or recalculate if needed)
        BigDecimal issueCost = invoice.getIssueCost() != null ? invoice.getIssueCost() : BigDecimal.ZERO;

        BigDecimal newTotalCost = issueCost.add(partsCost).subtract(actualDiscount);

        // Update mutable fields
        invoice.setPartsCost(partsCost);
        invoice.setDiscount(actualDiscount);
        invoice.setTotalCost(newTotalCost);
        invoice.setNotes(notes);

        return invoiceRepository.save(invoice);
    }

    @Transactional(readOnly = true)
    public Invoice getInvoiceByTaskId(String taskId) {
        return invoiceRepository.findByTaskId(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", "taskId", taskId));
    }

    @Transactional(readOnly = true)
    public Invoice getInvoiceById(String invoiceId) {
        return invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", "invoiceId", invoiceId));
    }

    @Transactional(readOnly = true)
    public List<InvoiceLineItem> getInvoiceLineItems(String invoiceId) {
        return invoiceLineItemRepository.findByInvoiceId(invoiceId);
    }

    @Transactional(readOnly = true)
    public Page<Invoice> getInvoicesBetweenDates(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        return invoiceRepository.findInvoicesBetween(startDate, endDate, pageable);
    }

    @Transactional(readOnly = true)
    public BigDecimal calculateTotalRevenue(LocalDateTime startDate, LocalDateTime endDate) {
        return invoiceRepository.calculateTotalRevenueBetween(startDate, endDate);
    }

    @Transactional
    public void deleteInvoice(String invoiceId) {
        List<InvoiceLineItem> lineItems = invoiceLineItemRepository.findByInvoiceId(invoiceId);
        if (!lineItems.isEmpty()) {
            invoiceLineItemRepository.deleteAll(lineItems);
        }
        invoiceRepository.deleteById(invoiceId);
    }

    @Transactional(readOnly = true)
    public Optional<Invoice> findInvoiceByTaskId(String taskId) {
        // Returns the Optional directly without throwing an exception if empty
        return invoiceRepository.findByTaskId(taskId);
    }
}