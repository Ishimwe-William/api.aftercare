package com.bunsen.api.aftercare.repository;

import com.bunsen.api.aftercare.model.ActivityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, String> {
    List<ActivityLog> findByUserId(String userId);

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
}