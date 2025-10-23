package com.bunsen.api.aftercare.repository;

import com.bunsen.api.aftercare.model.User;
import com.bunsen.api.aftercare.repository.base.BaseRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TechnicianRepository extends BaseRepository<User, String> {

    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = 'ROLE_TECHNICIAN' AND u.enabled = true")
    List<User> findAllTechnicians();

    @Query("SELECT u FROM User u JOIN FETCH u.roles WHERE u.id = :id")
    Optional<User> findTechnicianByIdWithRoles(@Param("id") String id);

    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = 'ROLE_TECHNICIAN' AND u.enabled = true AND u.status = true")
    List<User> findAvailableTechnicians();

    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = 'ROLE_TECHNICIAN' AND u.status = :status")
    List<User> findTechniciansByStatus(@Param("status") boolean status);

    @Query("SELECT u FROM User u " +
            "JOIN u.roles r " +
            "LEFT JOIN ServiceTask st ON st.technician.id = u.id AND st.status IN ('PENDING', 'IN_PROGRESS') " +
            "WHERE r.name = 'ROLE_TECHNICIAN' AND u.enabled = true " +
            "GROUP BY u.id " +
            "ORDER BY COUNT(st.id) ASC")
    List<User> findTechniciansOrderedByWorkload();

    @Query("SELECT COUNT(st) FROM ServiceTask st WHERE st.technician.id = :technicianId AND st.status IN ('PENDING', 'IN_PROGRESS')")
    Long countActiveTasksByTechnician(@Param("technicianId") String technicianId);

    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = 'ROLE_TECHNICIAN' AND " +
            "(LOWER(u.username) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<User> searchTechnicians(@Param("searchTerm") String searchTerm);

    @Query("SELECT u.id, u.username, u.fullName, " +
            "COUNT(CASE WHEN st.status = 'COMPLETED' THEN 1 END) as completedTasks, " +
            "COUNT(CASE WHEN st.status IN ('PENDING', 'IN_PROGRESS') THEN 1 END) as activeTasks, " +
            "AVG(CASE WHEN st.status = 'COMPLETED' AND st.completedAt IS NOT NULL " +
            "THEN TIMESTAMPDIFF(HOUR, st.createdAt, st.completedAt) END) as avgCompletionTime " +
            "FROM User u " +
            "JOIN u.roles r " +
            "LEFT JOIN ServiceTask st ON st.technician.id = u.id " +
            "WHERE r.name = 'ROLE_TECHNICIAN' " +
            "GROUP BY u.id, u.username, u.fullName")
    List<Object[]> getTechnicianPerformanceMetrics();

    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM User u JOIN u.roles r WHERE u.id = :userId AND r.name = 'ROLE_TECHNICIAN'")
    Boolean isTechnician(@Param("userId") String userId);
}