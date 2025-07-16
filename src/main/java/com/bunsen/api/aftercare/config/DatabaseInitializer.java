package com.bunsen.api.aftercare.config;

import com.bunsen.api.aftercare.enums.ERole;
import com.bunsen.api.aftercare.model.Role;
import com.bunsen.api.aftercare.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.Optional;

@Configuration
public class DatabaseInitializer {

    @Bean
    CommandLineRunner initDatabase(RoleRepository roleRepository) {
        return args -> {
            // Initialize default roles if they don't exist
            Arrays.stream(ERole.values()).forEach(role -> {
                Optional<Role> existingRole = roleRepository.findByName(role);
                if (existingRole.isEmpty()) {
                    Role newRole = new Role();
                    newRole.setName(role);
                    roleRepository.save(newRole);
                    System.out.println("Created role: " + role);
                }
            });
        };
    }
}