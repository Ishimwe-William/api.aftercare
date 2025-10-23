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
public interface TimestampedRepository<T, ID> extends JpaRepository<T, ID> {

    @Query("SELECT e FROM #{#entityName} e WHERE e.timestamp BETWEEN :startDate AND :endDate ORDER BY e.timestamp DESC")
    Page<T> findBetween(@Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate,
                        Pageable pageable);

    @Query("SELECT e FROM #{#entityName} e WHERE e.timestamp >= :date ORDER BY e.timestamp DESC")
    Page<T> findAfter(@Param("date") LocalDateTime date, Pageable pageable);

    @Query("SELECT e FROM #{#entityName} e ORDER BY e.timestamp DESC")
    Page<T> findRecent(Pageable pageable);
}