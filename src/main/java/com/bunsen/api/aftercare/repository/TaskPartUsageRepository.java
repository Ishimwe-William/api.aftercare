package com.bunsen.api.aftercare.repository;

import com.bunsen.api.aftercare.model.TaskPartUsage;
import com.bunsen.api.aftercare.repository.base.TimestampedRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskPartUsageRepository extends TimestampedRepository<TaskPartUsage, String> {

    List<TaskPartUsage> findByTaskId(String taskId);

    List<TaskPartUsage> findByPartId(String partId);

    @Query("SELECT tpu.part.name, SUM(tpu.quantityUsed) FROM TaskPartUsage tpu GROUP BY tpu.part.name ORDER BY SUM(tpu.quantityUsed) DESC")
    List<Object[]> findMostUsedParts();

    @Query("SELECT SUM(tpu.quantityUsed) FROM TaskPartUsage tpu WHERE tpu.part.id = :partId")
    Long getTotalQuantityUsedForPart(@Param("partId") String partId);
}