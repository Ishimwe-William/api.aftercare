package com.bunsen.api.aftercare.service;

import com.bunsen.api.aftercare.dto.MonitoringDTO.*;
import com.bunsen.api.aftercare.enums.ETaskStatus;
import com.bunsen.api.aftercare.exception.ResourceNotFoundException;
import com.bunsen.api.aftercare.exception.BadRequestException;
import com.bunsen.api.aftercare.model.*;
import com.bunsen.api.aftercare.repository.*;
import com.bunsen.api.aftercare.service.helper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MonitoringService {

    private final ServiceTaskRepository serviceTaskRepository;
    private final ActivityLogRepository activityLogRepository;
    private final InvoiceRepository invoiceRepository;
    private final TaskPartUsageRepository taskPartUsageRepository;
    private final TaskFilterHelper filterHelper;
    private final TaskStatisticsCalculator statsCalculator;
    private final TaskPriorityCalculator priorityCalculator;
    private final TaskMapperHelper mapperHelper;
    private final TaskReassignmentService reassignmentService;

    @Transactional(readOnly = true)
    public Page<ServiceCaseResponse> getServiceCases(MonitoringFilter filter, Pageable pageable) {
        Page<ServiceTask> tasks;

        if (filter.getStatus() != null && filter.getStartDate() != null) {
            tasks = serviceTaskRepository.findByStatusAndAssignedAfter(
                    filter.getStatus(), filter.getStartDate(), pageable);
            return tasks.map(mapperHelper::mapToServiceCaseResponse);
        } else if (filter.getStatus() != null) {
            Page<ServiceTask> allTasks = serviceTaskRepository.findAll(pageable);

            List<ServiceCaseResponse> filteredResponses = allTasks.getContent().stream()
                    .filter(task -> task.getStatus() == filter.getStatus())
                    .map(mapperHelper::mapToServiceCaseResponse)
                    .toList();
            return new PageImpl<>(
                    filteredResponses,
                    pageable,
                    allTasks.getTotalElements()
            );

        } else {
            tasks = serviceTaskRepository.findAll(pageable);
            return tasks.map(mapperHelper::mapToServiceCaseResponse);
        }
    }

    @Transactional(readOnly = true)
    public List<ServiceCaseResponse> getFilteredServiceCases(MonitoringFilter filter) {
        filterHelper.validateFilter(filter);

        List<ServiceTask> tasks = serviceTaskRepository.findAll();
        List<ServiceTask> filtered = filterHelper.filterTasks(tasks, filter);

        return filtered.stream()
                .map(mapperHelper::mapToServiceCaseResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CaseDetailsResponse getCaseDetails(String taskId) {
        if (taskId == null || taskId.trim().isEmpty()) {
            throw new BadRequestException("Task ID is required");
        }

        ServiceTask task = serviceTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceTask", "taskId", taskId));

        CaseDetailsResponse response = new CaseDetailsResponse();
        response.setCaseInfo(mapperHelper.mapToServiceCaseResponse(task));
        response.setPartsUsed(getPartUsageForTask(taskId));
        response.setActivityHistory(getActivityHistoryForTask(taskId));
        response.setInvoice(getInvoiceForTask(taskId));

        return response;
    }

    @Transactional
    public ServiceCaseResponse reassignTask(ReassignRequest request) {
        // Use the centralized reassignment service with email enabled
        TaskReassignmentService.ReassignmentResult result =
                reassignmentService.reassignTask(request, true); // Send email = true

        return mapperHelper.mapToServiceCaseResponse(result.getTask());
    }

    @Transactional(readOnly = true)
    public List<AlertResponse> getAlerts() {
        List<AlertResponse> alerts = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        addOverdueAlerts(alerts, now);
        addHighPriorityAlerts(alerts, now);

        return alerts;
    }

    @Transactional(readOnly = true)
    public MonitoringStats getStatistics() {
        List<ServiceTask> allTasks = serviceTaskRepository.findAll();

        TaskStatisticsCalculator.TaskStatistics baseStats = statsCalculator.calculateStatistics(allTasks);
        Double avgCompletionTime = serviceTaskRepository.calculateAverageCompletionTimeInHours();

        MonitoringStats stats = new MonitoringStats();
        stats.setTotalCases(baseStats.getTotalTasks());
        stats.setPendingCases(baseStats.getPendingTasks());
        stats.setInProgressCases(baseStats.getInProgressTasks());
        stats.setCompletedCases(baseStats.getCompletedTasks());
        stats.setOverdueCases(baseStats.getOverdueTasks());
        stats.setAverageCompletionTime(avgCompletionTime != null ? avgCompletionTime : 0.0);

        return stats;
    }

    @Transactional(readOnly = true)
    public List<ServiceTimelineData> getServiceTimeline(LocalDateTime startDate, LocalDateTime endDate) {
        filterHelper.validateDateRange(startDate, endDate);

        Map<LocalDateTime, ServiceTimelineData> timelineMap = initializeTimelineMap(startDate, endDate);
        populateTimelineData(timelineMap, startDate, endDate);

        return new ArrayList<>(timelineMap.values());
    }

    @Transactional(readOnly = true)
    public List<PeakHoursData> getPeakHoursData(LocalDateTime startDate, LocalDateTime endDate) {
        filterHelper.validateDateRange(startDate, endDate);

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

    private void addOverdueAlerts(List<AlertResponse> alerts, LocalDateTime now) {
        Pageable pageable = PageRequest.of(0, 10);
        Page<ServiceTask> overdueTasks = serviceTaskRepository.findOverdueTasks(now, pageable);

        for (ServiceTask task : overdueTasks) {
            long hoursOverdue = priorityCalculator.calculateHoursOverdue(task, now);
            AlertResponse alert = new AlertResponse();
            alert.setId(UUID.randomUUID().toString());
            alert.setCaseId(task.getId());
            alert.setMessage(String.format("Case %s is overdue by %d hours", task.getId(), hoursOverdue));
            alert.setSeverity(priorityCalculator.getAlertSeverity(hoursOverdue));
            alert.setTimestamp(now);
            alerts.add(alert);
        }
    }

    private void addHighPriorityAlerts(List<AlertResponse> alerts, LocalDateTime now) {
        List<ServiceTask> inProgressTasks = serviceTaskRepository.findByStatus(ETaskStatus.IN_PROGRESS);

        for (ServiceTask task : inProgressTasks) {
            if (priorityCalculator.shouldGenerateAlert(task, now)) {
                AlertResponse alert = new AlertResponse();
                alert.setId(UUID.randomUUID().toString());
                alert.setCaseId(task.getId());
                alert.setMessage(String.format("High priority case %s requires attention", task.getId()));
                alert.setSeverity("error");
                alert.setTimestamp(now);
                alerts.add(alert);
            }
        }
    }

    private Map<LocalDateTime, ServiceTimelineData> initializeTimelineMap(
            LocalDateTime startDate, LocalDateTime endDate) {
        Map<LocalDateTime, ServiceTimelineData> timelineMap = new TreeMap<>();

        LocalDateTime current = startDate.toLocalDate().atStartOfDay();
        while (!current.isAfter(endDate)) {
            timelineMap.put(current, new ServiceTimelineData(current, 0, 0, 0));
            current = current.plusDays(1);
        }

        return timelineMap;
    }

    private void populateTimelineData(Map<LocalDateTime, ServiceTimelineData> timelineMap,
                                      LocalDateTime startDate, LocalDateTime endDate) {
        List<ServiceTask> allTasks = serviceTaskRepository.findAll();

        for (ServiceTask task : allTasks) {
            if (task.getCreatedAt().isAfter(startDate) && task.getCreatedAt().isBefore(endDate)) {
                LocalDateTime dayStart = task.getCreatedAt().toLocalDate().atStartOfDay();
                ServiceTimelineData data = timelineMap.get(dayStart);

                if (data != null) {
                    updateTimelineDataForStatus(data, task.getStatus());
                }
            }
        }
    }

    private void updateTimelineDataForStatus(ServiceTimelineData data, ETaskStatus status) {
        switch (status) {
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

    private List<PartUsageInfo> getPartUsageForTask(String taskId) {
        List<TaskPartUsage> usages = taskPartUsageRepository.findByTaskId(taskId);
        return usages.stream().map(usage -> {
            PartUsageInfo info = new PartUsageInfo();
            info.setPartId(usage.getPart().getId());
            info.setPartName(usage.getPart().getName());
            info.setQuantityUsed(usage.getQuantityUsed());
            info.setUnitCost(usage.getPart().getCost());
            info.setTotalCost(usage.getPart().getCost().multiply(
                    java.math.BigDecimal.valueOf(usage.getQuantityUsed())));
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