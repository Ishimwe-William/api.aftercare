package com.bunsen.api.aftercare.controller;

import com.bunsen.api.aftercare.dto.ActivityLogDTO;
import com.bunsen.api.aftercare.service.ActivityLogService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/activity-logs")
@PreAuthorize("hasRole('ADMIN') or hasRole('TECHNICIAN') or hasRole('SUPERVISOR')")
public class ActivityLogController {

    private final ActivityLogService activityLogService;

    public ActivityLogController(ActivityLogService activityLogService) {
        this.activityLogService = activityLogService;
    }

    @GetMapping
    public ResponseEntity<Page<ActivityLogDTO>> getLogsBetweenDates(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String userId,
            Pageable pageable) {
        return ResponseEntity.ok(activityLogService.getLogsBetweenDates(startDate, endDate, action, userId, pageable));
    }

    @GetMapping("/recent")
    public ResponseEntity<Page<ActivityLogDTO>> getRecentLogs(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String userId,
            Pageable pageable) {
        return ResponseEntity.ok(activityLogService.getRecentLogs(action, userId, pageable));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ActivityLogDTO>> getLogsByUser(@PathVariable String userId) {
        return ResponseEntity.ok(activityLogService.getLogsByUser(userId));
    }
}