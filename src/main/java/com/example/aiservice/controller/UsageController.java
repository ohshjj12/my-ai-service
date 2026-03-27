package com.example.aiservice.controller;

import com.example.aiservice.dto.ApiResponse;
import com.example.aiservice.entity.ApiUsageLog;
import com.example.aiservice.repository.ApiUsageLogRepository;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * GPT API 비용 모니터링 API.
 * ROLE_ADMIN 권한 필요.
 */
@RestController
@RequestMapping("/api/admin/usage")
@RequiredArgsConstructor
public class UsageController {

    private final ApiUsageLogRepository apiUsageLogRepository;

    /**
     * 기간별 API 사용 요약
     * GET /api/admin/usage/summary?days=30
     */
    @GetMapping("/summary")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSummary(
            @RequestParam(defaultValue = "30") int days) {

        LocalDateTime from = LocalDateTime.now().minusDays(days);
        BigDecimal totalCost = apiUsageLogRepository.sumCostSince(from);
        Long totalTokens   = apiUsageLogRepository.sumTokensSince(from);

        Map<String, Object> summary = Map.of(
                "periodDays", days,
                "from", from.toString(),
                "totalCostUsd", totalCost != null ? totalCost : BigDecimal.ZERO,
                "totalTokens",  totalTokens  != null ? totalTokens  : 0L
        );
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    /**
     * 최근 N개 로그 조회
     * GET /api/admin/usage/logs?days=7
     */
    @GetMapping("/logs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<ApiUsageLog>>> getLogs(
            @RequestParam(defaultValue = "7") int days) {

        LocalDateTime from = LocalDateTime.now().minusDays(days);
        LocalDateTime to   = LocalDateTime.now();
        List<ApiUsageLog> logs = apiUsageLogRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(from, to);
        return ResponseEntity.ok(ApiResponse.success(logs));
    }
}
