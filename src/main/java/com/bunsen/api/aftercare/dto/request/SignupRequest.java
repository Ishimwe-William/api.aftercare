package com.bunsen.api.aftercare.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Set;

@Data
public class SignupRequest {
    @NotBlank
    @Size(min = 3, max = 20)
    private String username;
    @NotBlank
    @Size(min = 6, max = 40)
    private String password;
    @NotBlank
    @Size(max = 50)
    @Email
    private String email;
    @Size(max = 100)
    private String fullName;
    @Size(max = 15)
    private String phoneNumber;
    @Size(max = 100)
    private String companyName;
    @Size(max = 100)
    private String contactPerson;
    private Set<String> roles;
}