package com.bunsen.api.aftercare.model;

import com.bunsen.api.aftercare.enums.ECaseStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Entity
@Table(name = "service_cases")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceCase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Bike bike;

    @ManyToOne
    private User technician;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ECaseStatus status;

    @OneToMany(mappedBy = "serviceCase", cascade = CascadeType.ALL)
    private List<Task> tasks;

    @OneToOne(mappedBy = "serviceCase", cascade = CascadeType.ALL)
    private Invoice invoice;

    @Temporal(TemporalType.TIMESTAMP)
    private Date openedAt;

    @Temporal(TemporalType.TIMESTAMP)
    private Date closedAt;
}

