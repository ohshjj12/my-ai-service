package com.example.aiservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Redis List를 사용한 분석 요청 큐.
 * 큐 이름: "analysis:queue"
 * 값: PredictionResult ID (Long)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AnalysisQueueService {

    static final String QUEUE_KEY = "analysis:queue";

    @Qualifier("analysisQueueRedisTemplate")
    private final RedisTemplate<String, Long> redisTemplate;

    /** 분석 요청을 큐 왼쪽에 추가 */
    public void enqueue(Long predictionResultId) {
        redisTemplate.opsForList().leftPush(QUEUE_KEY, predictionResultId);
        log.debug("분석 큐 추가: predictionResultId={}", predictionResultId);
    }

    /**
     * 큐 오른쪽에서 꺼냄 (FIFO).
     * 항목이 없으면 null 반환.
     */
    public Long dequeue() {
        return redisTemplate.opsForList().rightPop(QUEUE_KEY);
    }

    /** 현재 큐 길이 조회 */
    public long queueSize() {
        Long size = redisTemplate.opsForList().size(QUEUE_KEY);
        return size != null ? size : 0L;
    }

    /** 큐 전체 목록 조회 (모니터링용) */
    public List<Long> peekAll() {
        Long size = redisTemplate.opsForList().size(QUEUE_KEY);
        if (size == null || size == 0) return List.of();
        return redisTemplate.opsForList().range(QUEUE_KEY, 0, size - 1);
    }
}
