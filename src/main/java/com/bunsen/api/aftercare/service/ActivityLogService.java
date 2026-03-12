package com.bunsen.api.aftercare.service;

import com.bunsen.api.aftercare.dto.ActivityLogDTO;
import com.bunsen.api.aftercare.exception.ResourceNotFoundException;
import com.bunsen.api.aftercare.model.ActivityLog;
import com.bunsen.api.aftercare.model.User;
import com.bunsen.api.aftercare.repository.ActivityLogRepository;
import com.bunsen.api.aftercare.repository.UserRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;
    private final UserRepository userRepository;

    public ActivityLogService(ActivityLogRepository activityLogRepository, UserRepository userRepository) {
        this.activityLogRepository = activityLogRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void createLog(String userId, String action, String details) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        ActivityLog log = new ActivityLog();
        log.setLogId(UUID.randomUUID().toString());
        log.setUser(user);
        log.setAction(action);
        log.setDetails(details);
        log.setTimestamp(LocalDateTime.now());
        activityLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public Page<ActivityLogDTO> getLogsBetweenDates(LocalDateTime startDate, LocalDateTime endDate,
                                                    String action, String userId, Pageable pageable) {
        Page<ActivityLog> logs;

        if (action != null && userId != null) {
            logs = activityLogRepository.findByTimestampBetweenAndActionContainingAndUserId(
                    startDate, endDate, action, userId, pageable);
        } else if (action != null) {
            logs = activityLogRepository.findByTimestampBetweenAndActionContaining(
                    startDate, endDate, action, pageable);
        } else if (userId != null) {
            logs = activityLogRepository.findByTimestampBetweenAndUserId(
                    startDate, endDate, userId, pageable);
        } else {
            logs = activityLogRepository.findLogsBetween(startDate, endDate, pageable);
        }

        return logs.map(ActivityLogDTO::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<ActivityLogDTO> getRecentLogs(String action, String userId, Pageable pageable) {
        Page<ActivityLog> logs;

        if (action != null && userId != null) {
            // Use LIKE so "USER_CREATED" also matches "USER_CREATED_ADMIN"
            logs = activityLogRepository.findByActionContainingAndUserIdOrderByTimestampDesc(
                    action, userId, pageable);
        } else if (action != null) {
            // Use LIKE so "USER_CREATED" also matches "USER_CREATED_ADMIN"
            logs = activityLogRepository.findByActionContainingOrderByTimestampDesc(action, pageable);
        } else if (userId != null) {
            logs = activityLogRepository.findByUserIdOrderByTimestampDesc(userId, pageable);
        } else {
            logs = activityLogRepository.findRecentLogs(pageable);
        }

        return logs.map(ActivityLogDTO::fromEntity);
    }

    @Transactional(readOnly = true)
    public List<ActivityLogDTO> getLogsByUser(String userId) {
        return activityLogRepository.findByUserId(userId).stream()
                .map(ActivityLogDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public int reassignLogs(String oldUserId, String newUserId) {
        return activityLogRepository.reassignLogs(oldUserId, newUserId);
    }

    /**
     * Returns all distinct action strings stored in the DB, sorted alphabetically.
     * Cached so repeated calls don't hit the database — cache is invalidated
     * whenever a new log is written (wire into createLog via @CacheEvict if needed,
     * or rely on the TTL configured in your CacheManager).
     */
    @Cacheable("activityLogActions")
    @Transactional(readOnly = true)
    public List<String> getDistinctActions() {
        return activityLogRepository.findDistinctActions();
    }
}