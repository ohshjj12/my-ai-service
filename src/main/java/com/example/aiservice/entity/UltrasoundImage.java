package com.example.aiservice.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "ultrasound_images")
@Data
@NoArgsConstructor
public class UltrasoundImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String originalFileName;

    @Column(nullable = false)
    private String contentType;

    @Column(nullable = false)
    private Long fileSize;

    private String storedFilePath;

    private Integer weekNumber; // 임신 주차

    @Column(nullable = false)
    private LocalDateTime uploadedAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pregnancy_id")
    private Pregnancy pregnancy;

    @OneToOne(mappedBy = "ultrasoundImage", cascade = CascadeType.ALL)
    private PredictionResult predictionResult;
}
