package com.bunsen.api.aftercare.repository;

import com.bunsen.api.aftercare.enums.ETaskStatus;
import com.bunsen.api.aftercare.model.ServiceTask;
import com.bunsen.api.aftercare.repository.base.BaseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.nio.channels.FileChannel;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ServiceTaskRepository extends BaseRepository<ServiceTask, String> {

    List<ServiceTask> findByStatus(ETaskStatus status);

    List<ServiceTask> findByTechnicianId(String technicianId);

    List<ServiceTask> findByMotorcycleId(String motorcycleId);

    List<ServiceTask> findByTechnicianIdAndStatus(String technicianId,
                                                  ETaskStatus status);

    @Query("SELECT st FROM ServiceTask st WHERE st.status = :status AND st.assignedAt >= :startDate")
    Page<ServiceTask> findByStatusAndAssignedAfter(@Param("status") ETaskStatus status,
                                                   @Param("startDate") LocalDateTime startDate,
                                                   Pageable pageable);

    @Query("SELECT st FROM ServiceTask st WHERE st.dueTime < :now AND st.status IN ('PENDING', 'IN_PROGRESS')")
    Page<ServiceTask> findOverdueTasks(@Param("now") LocalDateTime now, Pageable pageable);

    @Query("SELECT st FROM ServiceTask st WHERE st.completedAt BETWEEN :startDate AND :endDate")
    Page<ServiceTask> findCompletedTasksBetween(@Param("startDate") LocalDateTime startDate,
                                                @Param("endDate") LocalDateTime endDate,
                                                Pageable pageable);

    @Query("SELECT COUNT(st) FROM ServiceTask st WHERE st.technician.id = :technicianId AND st.status = 'COMPLETED'")
    Long countCompletedTasksByTechnician(@Param("technicianId") String technicianId);

    @Query("SELECT st FROM ServiceTask st WHERE st.technician.id = :technicianId AND st.completedAt BETWEEN :startDate AND :endDate")
    Page<ServiceTask> findTechnicianTasksInDateRange(@Param("technicianId") String technicianId,
                                                     @Param("startDate") LocalDateTime startDate,
                                                     @Param("endDate") LocalDateTime endDate,
                                                     Pageable pageable);

    @Query("SELECT AVG(TIMESTAMPDIFF(HOUR, st.createdAt, st.completedAt)) " +
            "FROM ServiceTask st " +
            "WHERE st.status = 'COMPLETED' AND st.completedAt IS NOT NULL")
    Double calculateAverageCompletionTimeInHours();

    @Query("SELECT st FROM ServiceTask st WHERE st.completedAt BETWEEN :startDate AND :endDate")
    Page<ServiceTask> findAllTasksInDateRange(@Param("startDate") LocalDateTime startDate,
                                              @Param("endDate") LocalDateTime endDate,
                                              Pageable pageable);

    long countByMotorcycleIdAndStatusNot(String motorcycleId, ETaskStatus eTaskStatus);
}