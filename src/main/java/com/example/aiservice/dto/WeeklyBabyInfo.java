package com.example.aiservice.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 주차별 아기 표준 발달 정보
 */
@Getter
@Builder
public class WeeklyBabyInfo {

    /** 임신 주차 */
    private int week;

    /** 평균 키 (mm), 없으면 null */
    private Double avgLengthMm;

    /** 평균 몸무게 (g), 없으면 null */
    private Double avgWeightG;

    /** 크기 비교 예시 */
    private String sizeComparison;

    /** 이 주차 대표 발달 특징 */
    private String developmentDesc;

    /** 엄마에게 전하는 한 마디 */
    private String momTip;
}
