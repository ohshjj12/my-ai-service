package com.example.aiservice.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "prediction_results")
@Data
@NoArgsConstructor
public class PredictionResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String predictedGender;

    private Double confidenceScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnalysisStatus analysisStatus = AnalysisStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String reportText;

    @Column(nullable = false)
    private LocalDateTime analyzedAt = LocalDateTime.now();

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ultrasound_image_id", nullable = false)
    private UltrasoundImage ultrasoundImage;
}
