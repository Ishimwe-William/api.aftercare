package com.bunsen.api.aftercare.repository;

import com.bunsen.api.aftercare.model.SparePart;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SparePartRepository extends JpaRepository<SparePart, String> {
    Optional<SparePart> findByName(String name);

    List<SparePart> findBySupplierName(String supplierName);

    @Query("SELECT sp FROM SparePart sp WHERE sp.quantityAvailable <= sp.lowStockThreshold")
    Page<SparePart> findLowStockParts(Pageable pageable);

    @Query("SELECT sp FROM SparePart sp WHERE sp.quantityAvailable = 0")
    Page<SparePart> findOutOfStockParts(Pageable pageable);

    @Query("SELECT sp FROM SparePart sp WHERE LOWER(sp.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<SparePart> searchByName(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT sp FROM SparePart sp ORDER BY sp.quantityAvailable ASC")
    Page<SparePart> findAllOrderByQuantityAsc(Pageable pageable);
}