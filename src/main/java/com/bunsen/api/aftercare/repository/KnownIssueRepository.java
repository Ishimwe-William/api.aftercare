package com.bunsen.api.aftercare.repository;

import com.bunsen.api.aftercare.model.KnownIssue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface KnownIssueRepository extends JpaRepository<KnownIssue, Long> {
    boolean existsByNameIgnoreCase(String name);
    Optional<KnownIssue> findByNameIgnoreCase(String name);
}
