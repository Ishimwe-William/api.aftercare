package com.bunsen.api.aftercare.repository;

import com.bunsen.api.aftercare.model.Motorcycle;
import com.bunsen.api.aftercare.repository.base.BaseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MotorcycleRepository extends BaseRepository<Motorcycle, String> {

    Optional<Motorcycle> findByQrCode(String qrCode);

    List<Motorcycle> findByStatus(Motorcycle.MotorcycleStatus status);

    List<Motorcycle> findByOwnerPhone(String ownerPhone);

    List<Motorcycle> findByOwnerEmail(String ownerEmail);

    boolean existsByQrCode(String qrCode);

    @Query("SELECT m FROM Motorcycle m WHERE m.status = 'IN_SERVICE'")
    Page<Motorcycle> findAllInService(Pageable pageable);

    @Query("SELECT m FROM Motorcycle m WHERE m.lastServiceDate < :date OR m.lastServiceDate IS NULL")
    Page<Motorcycle> findMotorcyclesNeedingService(@Param("date") LocalDateTime date, Pageable pageable);
}