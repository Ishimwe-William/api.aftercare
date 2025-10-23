package com.bunsen.api.aftercare.service;

import com.bunsen.api.aftercare.dto.MonitoringDTO.*;
import com.bunsen.api.aftercare.exception.ResourceNotFoundException;
import com.bunsen.api.aftercare.exception.BadRequestException;
import com.bunsen.api.aftercare.exception.ValidationException;
import com.bunsen.api.aftercare.model.*;
import com.bunsen.api.aftercare.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MonitoringService {

    private final ServiceTaskRepository serviceTaskRepository;
    private final ActivityLogRepository activityLogRepository;
    private final InvoiceRepository invoiceRepository;
    private final TaskPartUsageRepository taskPartUsageRepository;
    private final TechnicianRepository technicianRepository;
    private final MotorcycleRepository motorcycleRepository;

    @Transactional(readOnly = true)
    public Page<ServiceCaseResponse> getServiceCases(MonitoringFilter filter, Pageable pageable) {
        Page<ServiceTask> tasks;

        if (filter.getStatus() != null && filter.getStartDate() != null) {
            tasks = serviceTaskRepository.findByStatusAndAssignedAfter(
                    filter.getStatus(), filter.getStartDate(), pageable);
        } else if (filter.getStatus() != null) {
            tasks = serviceTaskRepository.findAll(pageable);
            tasks = tasks.map(task -> task.getStatus() == filter.getStatus() ? task : null)
                    .map(t -> t);
        } else {
            tasks = serviceTaskRepository.findAll(pageable);
        }

        return tasks.map(this::mapToServiceCaseResponse);
    }

    @Transactional(readOnly = true)
    public List<ServiceCaseResponse> getFilteredServiceCases(MonitoringFilter filter) {
        if (filter == null) {
            throw new BadRequestException("Filter parameters are required");
        }

        List<ServiceTask> tasks = serviceTaskRepository.findAll();

        return tasks.stream()
                .filter(task -> filterTask(task, filter))
                .map(this::mapToServiceCaseResponse)
                .collect(Collectors.toList());
    }

    private boolean filterTask(ServiceTask task, MonitoringFilter filter) {
        if (filter.getStatus() != null && task.getStatus() != filter.getStatus()) {
            return false;
        }
        if (filter.getTechnicianId() != null && !task.getTechnician().getId().equals(filter.getTechnicianId())) {
            return false;
        }
        if (filter.getMotorcycleId() != null && !task.getMotorcycle().getId().equals(filter.getMotorcycleId())) {
            return false;
        }
        if (filter.getStartDate() != null && task.getCreatedAt().isBefore(filter.getStartDate())) {
            return false;
        }
        if (filter.getEndDate() != null && task.getCreatedAt().isAfter(filter.getEndDate())) {
            return false;
        }
        return true;
    }

    @Transactional(readOnly = true)
    public CaseDetailsResponse getCaseDetails(String taskId) {
        if (taskId == null || taskId.trim().isEmpty()) {
            throw new BadRequestException("Task ID is required");
        }

        ServiceTask task = serviceTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceTask", "taskId", taskId));

        CaseDetailsResponse response = new CaseDetailsResponse();
        response.setCaseInfo(mapToServiceCaseResponse(task));
        response.setPartsUsed(getPartUsageForTask(taskId));
        response.setActivityHistory(getActivityHistoryForTask(taskId));
        response.setInvoice(getInvoiceForTask(taskId));

        return response;
    }

    @Transactional
    public ServiceCaseResponse reassignTask(ReassignRequest request) {
        if (request == null) {
            throw new BadRequestException("Reassignment request is required");
        }
        if (request.getTaskId() == null || request.getTaskId().trim().isEmpty()) {
            throw new ValidationException("Task ID is required");
        }
        if (request.getNewTechnicianId() == null || request.getNewTechnicianId().trim().isEmpty()) {
            throw new ValidationException("New technician ID is required");
        }

        ServiceTask task = serviceTaskRepository.findById(request.getTaskId())
                .orElseThrow(() -> new ResourceNotFoundException("ServiceTask", "taskId", request.getTaskId()));

        // Validate task can be reassigned
        if (task.getStatus() == ServiceTask.TaskStatus.COMPLETED) {
            throw new BadRequestException("Cannot reassign completed tasks");
        }

        User newTechnician = technicianRepository.findById(request.getNewTechnicianId())
                .orElseThrow(() -> new ResourceNotFoundException("Technician", "id", request.getNewTechnicianId()));

        // Verify user is actually a technician
        Boolean isTech = technicianRepository.isTechnician(request.getNewTechnicianId());
        if (isTech == null || !isTech) {
            throw new ValidationException("User is not a technician");
        }

        // Check if new technician is enabled and active
        if (!newTechnician.isEnabled() || !newTechnician.isStatus()) {
            throw new ValidationException("Selected technician is not available");
        }

        String oldTechnicianName = task.getTechnician().getFullName();
        task.setTechnician(newTechnician);
        task.setUpdatedAt(LocalDateTime.now());

        ServiceTask savedTask = serviceTaskRepository.save(task);

        // Log activity
        ActivityLog log = new ActivityLog();
        log.setLogId(UUID.randomUUID().toString());
        log.setUser(newTechnician);
        log.setAction("TASK_REASSIGNED");
        log.setDetails(String.format("Task %s reassigned from %s to %s. Reason: %s",
                task.getId(), oldTechnicianName, newTechnician.getFullName(),
                request.getReason() != null ? request.getReason() : "Not specified"));
        log.setTimestamp(LocalDateTime.now());
        activityLogRepository.save(log);

        return mapToServiceCaseResponse(savedTask);
    }

    @Transactional(readOnly = true)
    public List<AlertResponse> getAlerts() {
        List<AlertResponse> alerts = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // Find overdue tasks
        Pageable pageable = PageRequest.of(0, 10);
        Page<ServiceTask> overdueTasks = serviceTaskRepository.findOverdueTasks(now, pageable);

        for (ServiceTask task : overdueTasks) {
            long hoursOverdue = ChronoUnit.HOURS.between(task.getDueTime(), now);
            AlertResponse alert = new AlertResponse();
            alert.setId(UUID.randomUUID().toString());
            alert.setCaseId(task.getId());
            alert.setMessage(String.format("Case %s is overdue by %d hours", task.getId(), hoursOverdue));
            alert.setSeverity(hoursOverdue > 24 ? "error" : "warning");
            alert.setTimestamp(now);
            alerts.add(alert);
        }

        // Find high priority in-progress tasks
        List<ServiceTask> inProgressTasks = serviceTaskRepository.findByStatus(ServiceTask.TaskStatus.IN_PROGRESS);
        for (ServiceTask task : inProgressTasks) {
            if (task.getDueTime() != null && task.getDueTime().isBefore(now.plusHours(2))) {
                AlertResponse alert = new AlertResponse();
                alert.setId(UUID.randomUUID().toString());
                alert.setCaseId(task.getId());
                alert.setMessage(String.format("High priority case %s requires attention", task.getId()));
                alert.setSeverity("error");
                alert.setTimestamp(now);
                alerts.add(alert);
            }
        }

        return alerts;
    }

    @Transactional(readOnly = true)
    public MonitoringStats getStatistics() {
        List<ServiceTask> allTasks = serviceTaskRepository.findAll();
        LocalDateTime now = LocalDateTime.now();

        long total = allTasks.size();
        long pending = allTasks.stream().filter(t -> t.getStatus() == ServiceTask.TaskStatus.PENDING).count();
        long inProgress = allTasks.stream().filter(t -> t.getStatus() == ServiceTask.TaskStatus.IN_PROGRESS).count();
        long completed = allTasks.stream().filter(t -> t.getStatus() == ServiceTask.TaskStatus.COMPLETED).count();
        long overdue = allTasks.stream()
                .filter(t -> t.getDueTime() != null && t.getDueTime().isBefore(now)
                        && (t.getStatus() == ServiceTask.TaskStatus.PENDING || t.getStatus() == ServiceTask.TaskStatus.IN_PROGRESS))
                .count();

        Double avgCompletionTime = serviceTaskRepository.calculateAverageCompletionTimeInHours();

        MonitoringStats stats = new MonitoringStats();
        stats.setTotalCases(total);
        stats.setPendingCases(pending);
        stats.setInProgressCases(inProgress);
        stats.setCompletedCases(completed);
        stats.setOverdueCases(overdue);
        stats.setAverageCompletionTime(avgCompletionTime != null ? avgCompletionTime : 0.0);

        return stats;
    }

    @Transactional(readOnly = true)
    public List<ServiceTimelineData> getServiceTimeline(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null || endDate == null) {
            throw new BadRequestException("Start date and end date are required");
        }
        if (startDate.isAfter(endDate)) {
            throw new ValidationException("Start date must be before end date");
        }

        Pageable pageable = PageRequest.of(0, 1000);
        Page<ServiceTask> tasksInRange = serviceTaskRepository.findCompletedTasksBetween(startDate, endDate, pageable);

        Map<LocalDateTime, ServiceTimelineData> timelineMap = new TreeMap<>();

        LocalDateTime current = startDate.toLocalDate().atStartOfDay();
        while (!current.isAfter(endDate)) {
            timelineMap.put(current, new ServiceTimelineData(current, 0, 0, 0));
            current = current.plusDays(1);
        }

        List<ServiceTask> allTasks = serviceTaskRepository.findAll();
        for (ServiceTask task : allTasks) {
            if (task.getCreatedAt().isAfter(startDate) && task.getCreatedAt().isBefore(endDate)) {
                LocalDateTime dayStart = task.getCreatedAt().toLocalDate().atStartOfDay();
                ServiceTimelineData data = timelineMap.get(dayStart);
                if (data != null) {
                    switch (task.getStatus()) {
                        case PENDING:
                            data.setPendingCount(data.getPendingCount() + 1);
                            break;
                        case IN_PROGRESS:
                            data.setInProgressCount(data.getInProgressCount() + 1);
                            break;
                        case COMPLETED:
                            data.setCompletedCount(data.getCompletedCount() + 1);
                            break;
                    }
                }
            }
        }

        return new ArrayList<>(timelineMap.values());
    }

    @Transactional(readOnly = true)
    public List<PeakHoursData> getPeakHoursData(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null || endDate == null) {
            throw new BadRequestException("Start date and end date are required");
        }
        if (startDate.isAfter(endDate)) {
            throw new ValidationException("Start date must be before end date");
        }

        List<ServiceTask> allTasks = serviceTaskRepository.findAll();

        Map<String, PeakHoursData> peakMap = new HashMap<>();

        for (ServiceTask task : allTasks) {
            if (task.getCreatedAt().isAfter(startDate) && task.getCreatedAt().isBefore(endDate)) {
                int hour = task.getCreatedAt().getHour();
                int dayOfWeek = task.getCreatedAt().getDayOfWeek().getValue();
                String key = dayOfWeek + "-" + hour;

                peakMap.putIfAbsent(key, new PeakHoursData(hour, dayOfWeek, 0L));
                PeakHoursData data = peakMap.get(key);
                data.setTaskCount(data.getTaskCount() + 1);
            }
        }

        return new ArrayList<>(peakMap.values());
    }

    private ServiceCaseResponse mapToServiceCaseResponse(ServiceTask task) {
        ServiceCaseResponse response = new ServiceCaseResponse();
        response.setCaseId(task.getId());
        response.setIssue(task.getDescription());
        response.setIssueType(task.getIssueType());
        response.setDescription(task.getDescription());
        response.setTechnician(task.getTechnician().getFullName());
        response.setTechnicianId(task.getTechnician().getId());
        response.setStatus(task.getStatus());
        response.setProgress(calculateProgress(task));
        response.setStartTime(task.getStartedAt() != null ? task.getStartedAt() : task.getAssignedAt());
        response.setEndTime(task.getCompletedAt());
        response.setPriority(determinePriority(task));
        response.setLaborHours(task.getLaborHours());
        response.setEstimatedTime(task.getEstimatedTime());
        response.setDueTime(task.getDueTime());
        response.setNotes(task.getNotes());

        MotorcycleInfo motorcycleInfo = new MotorcycleInfo();
        Motorcycle motorcycle = task.getMotorcycle();
        motorcycleInfo.setId(motorcycle.getId());
        motorcycleInfo.setModel(motorcycle.getModel());
        motorcycleInfo.setPlateNumber(motorcycle.getPlateNumber());
        motorcycleInfo.setQrCode(motorcycle.getQrCode());
        motorcycleInfo.setOwnerName(motorcycle.getOwner().getName());
        motorcycleInfo.setOwnerPhone(motorcycle.getOwner().getPhone());
        response.setMotorcycle(motorcycleInfo);

        return response;
    }

    private Integer calculateProgress(ServiceTask task) {
        switch (task.getStatus()) {
            case PENDING:
                return 0;
            case IN_PROGRESS:
                if (task.getEstimatedTime() != null && task.getStartedAt() != null) {
                    long elapsed = ChronoUnit.MINUTES.between(task.getStartedAt(), LocalDateTime.now());
                    int progress = (int) ((elapsed * 100) / task.getEstimatedTime());
                    return Math.min(progress, 95);
                }
                return 50;
            case COMPLETED:
                return 100;
            case PAUSED:
                return 30;
            default:
                return 0;
        }
    }

    private String determinePriority(ServiceTask task) {
        if (task.getDueTime() != null) {
            long hoursUntilDue = ChronoUnit.HOURS.between(LocalDateTime.now(), task.getDueTime());
            if (hoursUntilDue < 0) return "critical";
            if (hoursUntilDue < 4) return "high";
            if (hoursUntilDue < 24) return "medium";
        }
        return "low";
    }

    private List<PartUsageInfo> getPartUsageForTask(String taskId) {
        List<TaskPartUsage> usages = taskPartUsageRepository.findByTaskId(taskId);
        return usages.stream().map(usage -> {
            PartUsageInfo info = new PartUsageInfo();
            info.setPartId(usage.getPart().getId());
            info.setPartName(usage.getPart().getName());
            info.setQuantityUsed(usage.getQuantityUsed());
            info.setUnitCost(usage.getPart().getCost());
            info.setTotalCost(usage.getPart().getCost().multiply(new java.math.BigDecimal(usage.getQuantityUsed())));
            info.setUsedAt(usage.getUsedAt());
            return info;
        }).collect(Collectors.toList());
    }

    private List<ActivityInfo> getActivityHistoryForTask(String taskId) {
        ServiceTask task = serviceTaskRepository.findById(taskId).orElse(null);
        if (task == null) return Collections.emptyList();

        List<ActivityLog> logs = activityLogRepository.findByUserId(task.getTechnician().getId());
        return logs.stream()
                .filter(log -> log.getDetails() != null && log.getDetails().contains(taskId))
                .map(log -> {
                    ActivityInfo info = new ActivityInfo();
                    info.setAction(log.getAction());
                    info.setDetails(log.getDetails());
                    info.setTimestamp(log.getTimestamp());
                    info.setPerformedBy(log.getUser().getFullName());
                    return info;
                })
                .collect(Collectors.toList());
    }

    private InvoiceInfo getInvoiceForTask(String taskId) {
        return invoiceRepository.findByTaskId(taskId)
                .map(invoice -> {
                    InvoiceInfo info = new InvoiceInfo();
                    info.setInvoiceId(invoice.getInvoiceId());
                    info.setInvoiceNumber(invoice.getInvoiceId());
                    info.setLaborCost(invoice.getLaborCost());
                    info.setPartsCost(invoice.getPartsCost());
                    info.setTotalCost(invoice.getTotalCost());
                    info.setDiscount(invoice.getDiscount());
                    info.setGeneratedAt(invoice.getGeneratedAt());
                    return info;
                })
                .orElse(null);
    }
}