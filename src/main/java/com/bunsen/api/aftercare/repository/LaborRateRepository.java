package com.bunsen.api.aftercare.repository;

import com.bunsen.api.aftercare.model.LaborRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LaborRateRepository extends JpaRepository<LaborRate, Long> {
    @Query("SELECT lr FROM LaborRate lr ORDER BY lr.createdAt DESC LIMIT 1")
    Optional<LaborRate> findMostRecent();

    Optional<LaborRate> findById(String rateId);
    void deleteById(String rateId);
}