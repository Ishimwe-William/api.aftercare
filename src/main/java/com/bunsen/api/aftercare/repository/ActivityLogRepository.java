package com.bunsen.api.aftercare.repository;

import com.bunsen.api.aftercare.model.ActivityLog;
import com.bunsen.api.aftercare.repository.base.TimestampedRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ActivityLogRepository extends TimestampedRepository<ActivityLog, String> {
    List<ActivityLog> findByUserId(String userId);

    @Modifying
    @Query("UPDATE ActivityLog a SET a.user.id = :newUserId WHERE a.user.id = :oldUserId")
    int reassignLogs(String oldUserId, String newUserId);

    List<ActivityLog> findByAction(String action);

    @Query("SELECT al FROM ActivityLog al WHERE al.timestamp BETWEEN :startDate AND :endDate ORDER BY al.timestamp DESC")
    Page<ActivityLog> findLogsBetween(@Param("startDate") LocalDateTime startDate,
                                      @Param("endDate") LocalDateTime endDate,
                                      Pageable pageable);

    @Query("SELECT al FROM ActivityLog al WHERE al.user.id = :userId AND al.timestamp >= :date ORDER BY al.timestamp DESC")
    Page<ActivityLog> findUserActivityAfter(@Param("userId") String userId,
                                            @Param("date") LocalDateTime date,
                                            Pageable pageable);

    @Query("SELECT al.action, COUNT(al) FROM ActivityLog al GROUP BY al.action ORDER BY COUNT(al) DESC")
    List<Object[]> findMostCommonActions();

    @Query("SELECT al FROM ActivityLog al ORDER BY al.timestamp DESC")
    Page<ActivityLog> findRecentLogs(Pageable pageable);


    Page<ActivityLog> findByUserIdOrderByTimestampDesc(String userId, Pageable pageable);


    Page<ActivityLog> findByTimestampBetweenAndUserId(LocalDateTime startDate, LocalDateTime endDate,
                                                      String userId, Pageable pageable);

    /**
     * "USER_CREATED" will match "USER_CREATED", "USER_CREATED_ADMIN", etc.
     */
    @Query("SELECT l FROM ActivityLog l WHERE l.action LIKE %:action% ORDER BY l.timestamp DESC")
    Page<ActivityLog> findByActionContainingOrderByTimestampDesc(@Param("action") String action,
                                                                 Pageable pageable);

    @Query("SELECT l FROM ActivityLog l WHERE l.action LIKE %:action% AND l.user.id = :userId ORDER BY l.timestamp DESC")
    Page<ActivityLog> findByActionContainingAndUserIdOrderByTimestampDesc(@Param("action") String action,
                                                                          @Param("userId") String userId,
                                                                          Pageable pageable);

    @Query("SELECT l FROM ActivityLog l WHERE l.timestamp BETWEEN :start AND :end AND l.action LIKE %:action% ORDER BY l.timestamp DESC")
    Page<ActivityLog> findByTimestampBetweenAndActionContaining(@Param("start") LocalDateTime start,
                                                                @Param("end") LocalDateTime end,
                                                                @Param("action") String action,
                                                                Pageable pageable);

    @Query("SELECT l FROM ActivityLog l WHERE l.timestamp BETWEEN :start AND :end AND l.action LIKE %:action% AND l.user.id = :userId ORDER BY l.timestamp DESC")
    Page<ActivityLog> findByTimestampBetweenAndActionContainingAndUserId(@Param("start") LocalDateTime start,
                                                                         @Param("end") LocalDateTime end,
                                                                         @Param("action") String action,
                                                                         @Param("userId") String userId,
                                                                         Pageable pageable);
    /**
     * Returns every unique action value in the table, alphabetically sorted.
     * Cached in ActivityLogService via @Cacheable("activityLogActions").
     */
    @Query("SELECT DISTINCT l.action FROM ActivityLog l ORDER BY l.action ASC")
    List<String> findDistinctActions();
}