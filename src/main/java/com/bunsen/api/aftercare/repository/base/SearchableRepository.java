package com.bunsen.api.aftercare.repository.base;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface SearchableRepository<T, ID> extends JpaRepository<T, ID> {

    Page<T> searchByName(String keyword, Pageable pageable);
}