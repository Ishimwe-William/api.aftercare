package com.bunsen.api.aftercare.service;

import com.bunsen.api.aftercare.exception.ResourceNotFoundException;
import com.bunsen.api.aftercare.model.ActivityLog;
import com.bunsen.api.aftercare.model.User;
import com.bunsen.api.aftercare.repository.ActivityLogRepository;
import com.bunsen.api.aftercare.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ActivityLogService {
    private final ActivityLogRepository activityLogRepository;
    private final UserRepository userRepository;

    public ActivityLogService(ActivityLogRepository activityLogRepository, UserRepository userRepository) {
        this.activityLogRepository = activityLogRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ActivityLog createLog(String userId, String action, String details) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        ActivityLog log = new ActivityLog();
        log.setLogId(UUID.randomUUID().toString());
        log.setUser(user);
        log.setAction(action);
        log.setDetails(details);
        log.setTimestamp(LocalDateTime.now());
        return activityLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public Page<ActivityLog> getLogsBetweenDates(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        return activityLogRepository.findLogsBetween(startDate, endDate, pageable);
    }

    @Transactional(readOnly = true)
    public List<ActivityLog> getLogsByUser(String userId) {
        return activityLogRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<Object[]> getMostCommonActions() {
        return activityLogRepository.findMostCommonActions();
    }

    /**
     * Reassigns all activity logs from one user to another.
     * This method is critical for maintaining referential integrity during user deletion.
     * * @param oldUserId The ID of the user being deleted.
     * @param newUserId The ID of the new user (SYSTEM_USER) to inherit the logs.
     * @return The number of records updated.
     */
    @Transactional // Required because the repository uses @Modifying
    public int reassignLogs(String oldUserId, String newUserId) {
        return activityLogRepository.reassignLogs(oldUserId, newUserId);
    }
}
