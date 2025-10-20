package com.bunsen.api.aftercare.specification;

import com.bunsen.api.aftercare.dto.MonitoringDTO.MonitoringFilter;
import com.bunsen.api.aftercare.model.ServiceTask;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class ServiceTaskSpecification {

    public static Specification<ServiceTask> filterBy(MonitoringFilter filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.getStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), filter.getStatus()));
            }
            if (filter.getTechnicianId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("technician").get("id"), filter.getTechnicianId()));
            }
            if (filter.getMotorcycleId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("motorcycle").get("motorcycleId"), filter.getMotorcycleId()));
            }
            if (filter.getStartDate() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), filter.getStartDate()));
            }
            if (filter.getEndDate() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), filter.getEndDate()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}