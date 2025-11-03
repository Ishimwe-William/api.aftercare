package com.bunsen.api.aftercare.repository;

import com.bunsen.api.aftercare.model.Invoice;
import com.bunsen.api.aftercare.repository.base.TimestampedRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends TimestampedRepository<Invoice, String> {

    Optional<Invoice> findByTaskId(String taskId);

    @Query("SELECT i FROM Invoice i WHERE i.generatedAt BETWEEN :startDate AND :endDate")
    Page<Invoice> findInvoicesBetween(@Param("startDate") LocalDateTime startDate,
                                      @Param("endDate") LocalDateTime endDate,
                                      Pageable pageable);

    @Query("SELECT COALESCE(SUM(i.totalCost), 0) FROM Invoice i WHERE i.generatedAt BETWEEN :startDate AND :endDate")
    BigDecimal calculateTotalRevenueBetween(@Param("startDate") LocalDateTime startDate,
                                            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT i FROM Invoice i WHERE i.totalCost >= :minAmount ORDER BY i.totalCost DESC")
    Page<Invoice> findHighValueInvoices(@Param("minAmount") BigDecimal minAmount, Pageable pageable);

    @Query("SELECT COUNT(i) FROM Invoice i WHERE i.generatedAt >= :date")
    Long countInvoicesGeneratedAfter(@Param("date") LocalDateTime date);
}
