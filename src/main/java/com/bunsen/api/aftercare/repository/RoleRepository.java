package com.bunsen.api.aftercare.repository;

import com.bunsen.api.aftercare.enums.ERole;
import com.bunsen.api.aftercare.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(ERole name);
}
