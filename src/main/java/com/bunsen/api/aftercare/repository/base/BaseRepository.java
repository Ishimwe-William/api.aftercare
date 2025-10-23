package com.bunsen.api.aftercare.repository.base;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

@NoRepositoryBean
public interface BaseRepository<T, ID> extends JpaRepository<T, ID> {

    @Query("SELECT e FROM #{#entityName} e WHERE e.createdAt BETWEEN :startDate AND :endDate")
    List<T> findCreatedBetween(@Param("startDate") LocalDateTime startDate,
                               @Param("endDate") LocalDateTime endDate);

    @Query("SELECT e FROM #{#entityName} e WHERE e.createdAt >= :date ORDER BY e.createdAt DESC")
    Page<T> findCreatedAfter(@Param("date") LocalDateTime date, Pageable pageable);

    @Query("SELECT COUNT(e) FROM #{#entityName} e WHERE e.createdAt >= :date")
    Long countCreatedAfter(@Param("date") LocalDateTime date);
}