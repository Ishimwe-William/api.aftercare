package com.bunsen.api.aftercare.service;

import com.bunsen.api.aftercare.exception.ResourceNotFoundException;
import com.bunsen.api.aftercare.exception.TaskStatusException;
import com.bunsen.api.aftercare.model.Invoice;
import com.bunsen.api.aftercare.model.ServiceTask;
import com.bunsen.api.aftercare.repository.InvoiceRepository;
import com.bunsen.api.aftercare.repository.ServiceTaskRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class InvoiceService {
    private final InvoiceRepository invoiceRepository;
    private final ServiceTaskRepository serviceTaskRepository;

    public InvoiceService(InvoiceRepository invoiceRepository, ServiceTaskRepository serviceTaskRepository) {
        this.invoiceRepository = invoiceRepository;
        this.serviceTaskRepository = serviceTaskRepository;
    }

    @Transactional
    public Invoice generateInvoice(String taskId, BigDecimal laborCost, BigDecimal partsCost, BigDecimal discount) {
        ServiceTask task = serviceTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceTask", "taskId", taskId));
        if (task.getStatus() != ServiceTask.TaskStatus.COMPLETED) {
            throw new TaskStatusException(task.getStatus().name(), "generate invoice");
        }
        BigDecimal totalCost = laborCost.add(partsCost).subtract(discount != null ? discount : BigDecimal.ZERO);
        Invoice invoice = new Invoice();
        invoice.setInvoiceId(UUID.randomUUID().toString());
        invoice.setTask(task);
        invoice.setLaborCost(laborCost);
        invoice.setPartsCost(partsCost);
        invoice.setDiscount(discount);
        invoice.setTotalCost(totalCost);
        invoice.setGeneratedAt(LocalDateTime.now());
        return invoiceRepository.save(invoice);
    }

    @Transactional(readOnly = true)
    public Invoice getInvoiceById(String invoiceId) {
        return invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", "invoiceId", invoiceId));
    }

    @Transactional(readOnly = true)
    public Page<Invoice> getInvoicesBetweenDates(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        return invoiceRepository.findInvoicesBetween(startDate, endDate, pageable);
    }

    @Transactional(readOnly = true)
    public BigDecimal calculateTotalRevenue(LocalDateTime startDate, LocalDateTime endDate) {
        return invoiceRepository.calculateTotalRevenueBetween(startDate, endDate);
    }
}
