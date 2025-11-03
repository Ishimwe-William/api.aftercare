package com.bunsen.api.aftercare.controller;

import com.bunsen.api.aftercare.model.LaborRate;
import com.bunsen.api.aftercare.service.LaborRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/labor-rates")
@RequiredArgsConstructor
public class LaborRateController {

    private final LaborRateService laborRateService;

    @PostMapping
    public ResponseEntity<LaborRate> createRate(@RequestBody LaborRate laborRate) {
        LaborRate savedRate = laborRateService.save(laborRate);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(savedRate.getId())
                .toUri();
        return ResponseEntity.created(location).body(savedRate);
    }

    @GetMapping("/{id}")
    public ResponseEntity<LaborRate> getRate(@PathVariable String id) {
        return laborRateService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<LaborRate>> getAllRates() {
        return ResponseEntity.ok(laborRateService.findAll());
    }

    @GetMapping("/recent")
    public ResponseEntity<LaborRate> getRecentRate() {
        return laborRateService.getRecentRate()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<LaborRate> updateRate(@PathVariable String id, @RequestBody LaborRate laborRate) {
        return laborRateService.findById(id)
                .map(existingRate -> {
                    laborRate.setId(id);
                    return ResponseEntity.ok(laborRateService.save(laborRate));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRate(@PathVariable String id) {
        return laborRateService.findById(id)
                .map(rate -> {
                    laborRateService.delete(id);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
