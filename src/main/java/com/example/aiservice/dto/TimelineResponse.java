package com.example.aiservice.dto;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class TimelineResponse {
    private Long pregnancyId;
    private String nickname;
    /** 현재 또는 가장 최근 주차 기준 아기 표준 정보 */
    private WeeklyBabyInfo currentWeekInfo;
    private List<TimelineItemResponse> items;

    @Getter
    @Builder
    public static class TimelineItemResponse {
        private Long imageId;
        private String originalFileName;
        private Integer weekNumber;
        private LocalDateTime uploadedAt;

        // 분석 결과 (null 가능 — 분석 전)
        private Long predictionId;
        private String predictedGender;
        private Double confidenceScore;
        private String analysisStatus;
        private String reportText;
        private LocalDateTime analyzedAt;

        /** 해당 주차 아기 표준 정보 */
        private WeeklyBabyInfo babyInfo;
    }
}
