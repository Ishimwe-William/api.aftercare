package com.bunsen.api.aftercare.service.helper;

import com.bunsen.api.aftercare.dto.MonitoringDTO.*;
import com.bunsen.api.aftercare.model.Motorcycle;
import com.bunsen.api.aftercare.model.ServiceTask;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Component for mapping ServiceTask to various response DTOs
 * Centralizes mapping logic used in MonitoringService
 */
@Component
@RequiredArgsConstructor
public class TaskMapperHelper {

    private final TaskPriorityCalculator priorityCalculator;

    public ServiceCaseResponse mapToServiceCaseResponse(ServiceTask task) {
        ServiceCaseResponse response = new ServiceCaseResponse();
        response.setCaseId(task.getId());
        response.setIssue(task.getDescription());
        response.setIssueType(task.getIssueType());
        response.setDescription(task.getDescription());
        response.setTechnician(task.getTechnician().getFullName());
        response.setTechnicianId(task.getTechnician().getId());
        response.setStatus(task.getStatus());
        response.setProgress(priorityCalculator.calculateProgress(task));
        response.setStartTime(task.getStartedAt() != null ? task.getStartedAt() : task.getAssignedAt());
        response.setEndTime(task.getCompletedAt());
        response.setPriority(priorityCalculator.determinePriority(task));
        response.setLaborHours(task.getLaborHours());
        response.setEstimatedTime(task.getEstimatedTime());
        response.setDueTime(task.getDueTime());
        response.setNotes(task.getNotes());
        response.setCreatedAt(task.getCreatedAt());

        response.setMotorcycle(mapToMotorcycleInfo(task.getMotorcycle()));

        return response;
    }

    public MotorcycleInfo mapToMotorcycleInfo(Motorcycle motorcycle) {
        MotorcycleInfo info = new MotorcycleInfo();
        info.setId(motorcycle.getId());
        info.setModel(motorcycle.getModel());
        info.setPlateNumber(motorcycle.getPlateNumber());
        info.setQrCode(motorcycle.getQrCode());
        info.setOwnerName(motorcycle.getOwner().getName());
        info.setOwnerPhone(motorcycle.getOwner().getPhone());
        return info;
    }
}