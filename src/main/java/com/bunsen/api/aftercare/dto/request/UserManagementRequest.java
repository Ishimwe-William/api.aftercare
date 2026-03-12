package com.bunsen.api.aftercare.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Date;
import java.util.Set;

@Data
public class UserManagementRequest {
    private String id;

    @NotBlank
    private String username;
    @NotBlank
    @Email
    private String email;
    private Set<String> roles;
    private boolean enabled;
    private String fullName;
    private String photoUrl;
    private String phoneNumber;
    private Date updatedAt;
    private boolean passwordChangeRequired;
    private String speciality;
    private boolean status;
}