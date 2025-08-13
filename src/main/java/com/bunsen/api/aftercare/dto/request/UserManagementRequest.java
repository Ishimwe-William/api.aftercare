package com.bunsen.api.aftercare.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Set;

@Data
public class UserManagementRequest {
    @NotBlank
    private String username;
    
    @NotBlank
    @Email
    private String email;
    
    private Set<String> roles;
    
    private boolean enabled;
}