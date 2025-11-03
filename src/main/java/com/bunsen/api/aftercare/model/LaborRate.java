package com.bunsen.api.aftercare.model;

import com.bunsen.api.aftercare.model.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "labor_rate")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class LaborRate extends BaseEntity {
    @Column(name = "rate", precision = 10, scale = 2)
    private BigDecimal rate = BigDecimal.ZERO;
}