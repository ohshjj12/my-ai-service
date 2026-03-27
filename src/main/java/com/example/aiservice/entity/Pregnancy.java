package com.example.aiservice.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "pregnancies")
@Data
@NoArgsConstructor
public class Pregnancy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nickname; // 태명

    @Column(nullable = false)
    private LocalDate dueDate; // 출산 예정일

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "pregnancy", cascade = CascadeType.ALL)
    @OrderBy("weekNumber ASC, uploadedAt ASC")
    private List<UltrasoundImage> ultrasoundImages;
}
