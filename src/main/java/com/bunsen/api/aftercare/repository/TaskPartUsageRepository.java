package com.bunsen.api.aftercare.repository;

import com.bunsen.api.aftercare.model.TaskPartUsage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TaskPartUsageRepository extends JpaRepository<TaskPartUsage, String> {
    List<TaskPartUsage> findByTaskTaskId(String taskId);

    List<TaskPartUsage> findByPartPartId(String partId);

    @Query("SELECT tpu FROM TaskPartUsage tpu WHERE tpu.usedAt BETWEEN :startDate AND :endDate")
    Page<TaskPartUsage> findUsagesBetween(@Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate,
                                          Pageable pageable);

    @Query("SELECT tpu.part.name, SUM(tpu.quantityUsed) FROM TaskPartUsage tpu GROUP BY tpu.part.name ORDER BY SUM(tpu.quantityUsed) DESC")
    List<Object[]> findMostUsedParts();

    @Query("SELECT SUM(tpu.quantityUsed) FROM TaskPartUsage tpu WHERE tpu.part.partId = :partId")
    Long getTotalQuantityUsedForPart(@Param("partId") String partId);
}