package com.example.aiservice.controller;

import com.example.aiservice.dto.ApiResponse;
import com.example.aiservice.dto.PredictionResponse;
import com.example.aiservice.service.PredictionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/predictions")
@RequiredArgsConstructor
public class PredictionController {

    private final PredictionService predictionService;

    /**
     * 초음파 이미지 업로드 + 분석 큐 등록.
     * 분석은 백그라운드에서 처리되며 즉시 202 Accepted를 반환합니다.
     * 클라이언트는 GET /api/predictions/{id}/status 로 완료 여부를 확인합니다.
     */
    @PostMapping("/analyze")
    public ResponseEntity<ApiResponse<PredictionResponse>> analyzeImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) Long pregnancyId,
            @RequestParam(required = false) Integer weekNumber,
            Authentication authentication) throws IOException {
        PredictionResponse response = predictionService.enqueueAnalysis(
                file, authentication.getName(), pregnancyId, weekNumber);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success("분석 요청이 접수되었습니다. predictionId로 상태를 확인하세요.", response));
    }

    /**
     * 내 분석 결과 목록 조회.
     */
    @GetMapping("/my-results")
    public ResponseEntity<ApiResponse<List<PredictionResponse>>> getMyResults(Authentication authentication) {
        List<PredictionResponse> results = predictionService.getResultsByUser(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    /**
     * 특정 분석 결과의 상태 조회.
     * GET /api/predictions/{id}/status
     */
    @GetMapping("/{id}/status")
    public ResponseEntity<ApiResponse<PredictionResponse>> getStatus(
            @PathVariable Long id,
            Authentication authentication) {
        PredictionResponse response = predictionService.getResultById(id, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

