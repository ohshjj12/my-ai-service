package com.example.aiservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Redis 분석 큐를 소비하는 백그라운드 워커.
 * 1초마다 큐를 확인해 미처리 항목이 있으면 AI 분석을 수행합니다.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AnalysisWorkerService {

    private final AnalysisQueueService analysisQueueService;
    private final PredictionService predictionService;

    /**
     * 1초 간격으로 큐 폴링.
     * fixedDelay: 이전 실행 완료 후 1초 대기 (동시 실행 방지).
     */
    @Scheduled(fixedDelay = 1000)
    public void processQueue() {
        Long predictionResultId = analysisQueueService.dequeue();
        if (predictionResultId == null) return; // 큐 비어있음

        log.debug("큐에서 작업 수신: predictionResultId={}", predictionResultId);
        try {
            predictionService.performAnalysis(predictionResultId);
        } catch (Exception e) {
            log.error("워커 처리 중 예외 발생: predictionResultId={}, error={}",
                    predictionResultId, e.getMessage(), e);
            // 예외가 발생해도 워커는 계속 실행 (스케줄 유지)
        }
    }
}
