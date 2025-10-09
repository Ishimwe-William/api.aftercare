package com.bunsen.api.aftercare.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "blacklisted_tokens")
public class BlacklistedToken {
    @Id
    private String tokenId;
    @Column(name = "blacklisted_at")
    private LocalDateTime blacklistedAt;
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
}