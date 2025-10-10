package com.bunsen.api.aftercare.service;

import com.bunsen.api.aftercare.exception.LowStockException;
import com.bunsen.api.aftercare.exception.ResourceNotFoundException;
import com.bunsen.api.aftercare.model.ServiceTask;
import com.bunsen.api.aftercare.model.SparePart;
import com.bunsen.api.aftercare.model.TaskPartUsage;
import com.bunsen.api.aftercare.repository.ServiceTaskRepository;
import com.bunsen.api.aftercare.repository.SparePartRepository;
import com.bunsen.api.aftercare.repository.TaskPartUsageRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class TaskPartUsageService {
    private final TaskPartUsageRepository taskPartUsageRepository;
    private final ServiceTaskRepository serviceTaskRepository;
    private final SparePartRepository sparePartRepository;

    public TaskPartUsageService(TaskPartUsageRepository taskPartUsageRepository,
                                ServiceTaskRepository serviceTaskRepository,
                                SparePartRepository sparePartRepository) {
        this.taskPartUsageRepository = taskPartUsageRepository;
        this.serviceTaskRepository = serviceTaskRepository;
        this.sparePartRepository = sparePartRepository;
    }

    @Transactional
    public TaskPartUsage logPartUsage(String taskId, String partId, int quantityUsed) {
        if (quantityUsed <= 0) {
            throw new IllegalArgumentException("Quantity used must be positive");
        }
        ServiceTask task = serviceTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceTask", "taskId", taskId));
        SparePart part = sparePartRepository.findById(partId)
                .orElseThrow(() -> new ResourceNotFoundException("SparePart", "partId", partId));
        if (part.getQuantityAvailable() < quantityUsed) {
            throw new LowStockException(part.getName(), part.getQuantityAvailable(), quantityUsed);
        }
        part.setQuantityAvailable(part.getQuantityAvailable() - quantityUsed);
        sparePartRepository.save(part);
        TaskPartUsage usage = new TaskPartUsage();
        usage.setUsageId(UUID.randomUUID().toString());
        usage.setTask(task);
        usage.setPart(part);
        usage.setQuantityUsed(quantityUsed);
        usage.setUsedAt(LocalDateTime.now());
        return taskPartUsageRepository.save(usage);
    }

    @Transactional(readOnly = true)
    public List<TaskPartUsage> getUsagesByTask(String taskId) {
        return taskPartUsageRepository.findByTaskTaskId(taskId);
    }

    @Transactional(readOnly = true)
    public Page<TaskPartUsage> getUsagesBetweenDates(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        return taskPartUsageRepository.findUsagesBetween(startDate, endDate, pageable);
    }

    @Transactional(readOnly = true)
    public List<Object[]> getMostUsedParts() {
        return taskPartUsageRepository.findMostUsedParts();
    }
}
