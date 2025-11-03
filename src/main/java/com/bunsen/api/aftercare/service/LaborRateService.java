package com.bunsen.api.aftercare.service;

import com.bunsen.api.aftercare.model.LaborRate;
import com.bunsen.api.aftercare.repository.LaborRateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class LaborRateService {
    private final LaborRateRepository laborRateRepository;

    public LaborRate save(LaborRate laborRate) {
        return laborRateRepository.save(laborRate);
    }

    public Optional<LaborRate> findById(String id) {
        return laborRateRepository.findById(id);
    }

    public List<LaborRate> findAll() {
        return laborRateRepository.findAll();
    }

    public void delete(String id) {
        laborRateRepository.deleteById(id);
    }

    public Optional<LaborRate> getRecentRate() {
        return laborRateRepository.findMostRecent();
    }
}
