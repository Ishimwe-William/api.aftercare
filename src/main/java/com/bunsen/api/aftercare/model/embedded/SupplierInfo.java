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
public class SupplierInfo {
    @Column(name = "supplier_name", length = 100)
    private String name;

    @Column(name = "supplier_contact", length = 255)
    private String contact;
}