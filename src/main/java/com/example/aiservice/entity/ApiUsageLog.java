package com.example.aiservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * OpenAI API 호출 비용 로그.
 * 모델별 토큰 사용량과 예상 비용(USD)을 기록합니다.
 */
@Entity
@Table(name = "api_usage_logs", indexes = {
        @Index(name = "idx_usage_created_at", columnList = "createdAt")
})
@Getter
@NoArgsConstructor
public class ApiUsageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 사용 모델 (gpt-4o, gpt-4o-mini 등) */
    @Column(nullable = false)
    private String model;

    /** 분석 결과 ID (연결 추적용, nullable) */
    private Long predictionResultId;

    /** 입력 토큰 수 (이미지 포함) */
    @Column(nullable = false)
    private int promptTokens;

    /** 출력 토큰 수 */
    @Column(nullable = false)
    private int completionTokens;

    /** 전체 토큰 수 */
    @Column(nullable = false)
    private int totalTokens;

    /**
     * 예상 비용 (USD).
     * gpt-4o 기준: input $5/1M tokens, output $15/1M tokens
     */
    @Column(nullable = false, precision = 10, scale = 6)
    private BigDecimal estimatedCostUsd;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public ApiUsageLog(String model, Long predictionResultId,
                       int promptTokens, int completionTokens, int totalTokens,
                       BigDecimal estimatedCostUsd) {
        this.model = model;
        this.predictionResultId = predictionResultId;
        this.promptTokens = promptTokens;
        this.completionTokens = completionTokens;
        this.totalTokens = totalTokens;
        this.estimatedCostUsd = estimatedCostUsd;
    }
}
