package com.bunsen.api.aftercare.service;

import com.bunsen.api.aftercare.dto.KnownIssueDTO;
import com.bunsen.api.aftercare.exception.ResourceNotFoundException;
import com.bunsen.api.aftercare.model.KnownIssue;
import com.bunsen.api.aftercare.repository.KnownIssueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KnownIssueService {
    private final KnownIssueRepository knownIssueRepository;

    public List<KnownIssueDTO.Response> getAllIssues() {
        return knownIssueRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public KnownIssueDTO.Response createIssue(KnownIssueDTO.CreateRequest request) {
        if (knownIssueRepository.existsByNameIgnoreCase(request.getName())) {
            throw new DataIntegrityViolationException("Issue with this name already exists");
        }

        KnownIssue issue = new KnownIssue();
        issue.setName(request.getName());
        issue.setPrice(request.getPrice());

        return mapToResponse(knownIssueRepository.save(issue));
    }

    @Transactional
    public KnownIssueDTO.Response updateIssue(Long id, KnownIssueDTO.UpdateRequest request) {
        KnownIssue issue = knownIssueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Known issue not found"));

        issue.setPrice(request.getPrice());
        return mapToResponse(knownIssueRepository.save(issue));
    }

    @Transactional
    public void deleteIssue(Long id) {
        if (!knownIssueRepository.existsById(id)) {
            throw new ResourceNotFoundException("Known issue not found");
        }
        knownIssueRepository.deleteById(id);
    }

    private KnownIssueDTO.Response mapToResponse(KnownIssue issue) {
        KnownIssueDTO.Response response = new KnownIssueDTO.Response();
        response.setId(issue.getId());
        response.setName(issue.getName());
        response.setPrice(issue.getPrice());
        response.setCreatedAt(issue.getCreatedAt());
        response.setUpdatedAt(issue.getUpdatedAt());
        return response;
    }
}
