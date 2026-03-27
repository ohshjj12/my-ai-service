package com.example.aiservice.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiAnalysisResult {
    private String predictedGender;   // "MALE" or "FEMALE"
    private Double confidenceScore;   // 0.60 ~ 0.99
    private String reportText;        // 한국어 분석 리포트
}
