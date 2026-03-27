package com.example.aiservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Redis List를 사용한 분석 요청 큐.
 * 큐 이름: "analysis:queue"
 * 값: PredictionResult ID (Long)
 *
 * app.redis.enabled=false 이면 no-op으로 동작 (로컬 개발용).
 */
@Service
@Slf4j
public class AnalysisQueueService {

    static final String QUEUE_KEY = "analysis:queue";

    private final RedisTemplate<String, Long> redisTemplate;
    private final boolean redisEnabled;

    // Redis 활성화 시 생성자
    public AnalysisQueueService(
            @Qualifier("analysisQueueRedisTemplate")
            org.springframework.beans.factory.ObjectProvider<RedisTemplate<String, Long>> redisTemplateProvider,
            @org.springframework.beans.factory.annotation.Value("${app.redis.enabled:true}") boolean redisEnabled) {
        this.redisEnabled = redisEnabled;
        this.redisTemplate = redisEnabled ? redisTemplateProvider.getIfAvailable() : null;
        if (!redisEnabled) {
            log.warn("Redis 비활성화 상태입니다. 분석 큐 기능이 동작하지 않습니다.");
        }
    }

    /** Redis 활성화 여부 반환 */
    public boolean isEnabled() {
        return redisEnabled;
    }

    /** 분석 요청을 큐 왼쪽에 추가 */
    public void enqueue(Long predictionResultId) {
        if (!redisEnabled) {
            log.warn("Redis 비활성화 - enqueue 무시: predictionResultId={}", predictionResultId);
            return;
        }
        redisTemplate.opsForList().leftPush(QUEUE_KEY, predictionResultId);
        log.debug("분석 큐 추가: predictionResultId={}", predictionResultId);
    }

    /**
     * 큐 오른쪽에서 꺼냄 (FIFO).
     * 항목이 없으면 null 반환.
     */
    public Long dequeue() {
        if (!redisEnabled) return null;
        return redisTemplate.opsForList().rightPop(QUEUE_KEY);
    }

    /** 현재 큐 길이 조회 */
    public long queueSize() {
        if (!redisEnabled) return 0L;
        Long size = redisTemplate.opsForList().size(QUEUE_KEY);
        return size != null ? size : 0L;
    }

    /** 큐 전체 목록 조회 (모니터링용) */
    public List<Long> peekAll() {
        if (!redisEnabled) return List.of();
        Long size = redisTemplate.opsForList().size(QUEUE_KEY);
        if (size == null || size == 0) return List.of();
        return redisTemplate.opsForList().range(QUEUE_KEY, 0, size - 1);
    }
}
