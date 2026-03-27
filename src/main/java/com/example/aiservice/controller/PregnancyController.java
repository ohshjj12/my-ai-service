package com.example.aiservice.controller;

import com.example.aiservice.dto.ApiResponse;
import com.example.aiservice.dto.PregnancyRequest;
import com.example.aiservice.dto.PregnancyResponse;
import com.example.aiservice.dto.TimelineResponse;
import com.example.aiservice.service.PregnancyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pregnancies")
@RequiredArgsConstructor
public class PregnancyController {

    private final PregnancyService pregnancyService;

    /**
     * 임신 등록
     * POST /api/pregnancies
     * Body: { "nickname": "태명", "dueDate": "2026-10-01" }
     */
    @PostMapping
    public ResponseEntity<ApiResponse<PregnancyResponse>> create(
            @Valid @RequestBody PregnancyRequest request,
            Authentication authentication) {
        PregnancyResponse resp = pregnancyService.create(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("임신 정보 등록 완료", resp));
    }

    /**
     * 내 임신 목록 조회
     * GET /api/pregnancies
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<PregnancyResponse>>> findAll(Authentication authentication) {
        List<PregnancyResponse> list = pregnancyService.findAll(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    /**
     * 성장 타임라인 조회
     * GET /api/pregnancies/{id}/timeline
     */
    @GetMapping("/{id}/timeline")
    public ResponseEntity<ApiResponse<TimelineResponse>> getTimeline(
            @PathVariable Long id,
            Authentication authentication) {
        TimelineResponse timeline = pregnancyService.getTimeline(authentication.getName(), id);
        return ResponseEntity.ok(ApiResponse.success(timeline));
    }
}
