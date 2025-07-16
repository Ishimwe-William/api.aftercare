package com.bunsen.api.aftercare.model;

import com.bunsen.api.aftercare.enums.ETaskStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Entity
@Table(name = "tasks")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ETaskStatus status; // Pending, In Progress, Done

    @ManyToOne
    private ServiceCase serviceCase;

    private String notes;
    private String timeSpent;

    @ManyToMany
    @JoinTable(name = "task_parts",
            joinColumns = @JoinColumn(name = "task_id"),
            inverseJoinColumns = @JoinColumn(name = "part_id"))
    private Set<Part> partsUsed;

    private String photoUrl; // if storing one photo, else make a new Photo entity
}
