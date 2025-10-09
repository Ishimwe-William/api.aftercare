package com.bunsen.api.aftercare.dto.response;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Getter
@Setter
public class JwtResponse {
    private String token;
    private String refreshToken;
    private String type = "Bearer";
    private String id;
    private String username;
    private String fullName;
    private String photoUrl;
    private String phoneNumber;
    private String email;
    private LocalDateTime updatedAt;
    private List<String> roles;
    private boolean passwordChangeRequired;
    private boolean enabled;
    private boolean status;

    public JwtResponse(String accessToken, String refreshToken, String id, String fullName, String username, String email, List<String> roles, String phoneNumber, String photoUrl, LocalDateTime updatedAt, boolean passwordChangeRequired, boolean enabled, boolean status) {
        this.token = accessToken;
        this.refreshToken = refreshToken;
        this.id = id;
        this.username = username;
        this.email = email;
        this.roles = roles;
        this.phoneNumber = phoneNumber;
        this.photoUrl = photoUrl;
        this.fullName = fullName;
        this.updatedAt = updatedAt;
        this.passwordChangeRequired = passwordChangeRequired;
        this.enabled = enabled;
        this.status = status;
    }
}