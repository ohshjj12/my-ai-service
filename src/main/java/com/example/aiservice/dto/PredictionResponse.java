package com.example.aiservice.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class PredictionResponse {
    private Long predictionId;
    private Long imageId;
    private String originalFileName;
    private String predictedGender;
    private Double confidenceScore;
    private LocalDateTime analyzedAt;
    private String uploadedBy;
}
