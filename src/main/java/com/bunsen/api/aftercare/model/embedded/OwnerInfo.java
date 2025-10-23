package com.bunsen.api.aftercare.model.embedded;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OwnerInfo {
    @Column(name = "owner_name", length = 100)
    private String name;

    @Column(name = "owner_phone", length = 20)
    private String phone;

    @Column(name = "owner_email", length = 255)
    private String email;
}