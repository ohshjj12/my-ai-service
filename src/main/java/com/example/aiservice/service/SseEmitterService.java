package com.example.aiservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSE(Server-Sent Events) Emitter 관리 서비스.
 * 분석 완료 시 해당 사용자의 연결에 이벤트를 전송합니다.
 */
@Service
@Slf4j
public class SseEmitterService {

    // key: "username:predictionId"
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    private static final long SSE_TIMEOUT_MS = 60_000L; // 60초

    /**
     * 클라이언트가 SSE 연결 요청 시 Emitter를 생성하고 등록합니다.
     */
    public SseEmitter subscribe(String username, Long predictionId) {
        String key = buildKey(username, predictionId);
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);

        emitter.onCompletion(() -> {
            emitters.remove(key);
            log.debug("SSE 연결 종료: {}", key);
        });
        emitter.onTimeout(() -> {
            emitters.remove(key);
            log.debug("SSE 타임아웃: {}", key);
        });
        emitter.onError(e -> {
            emitters.remove(key);
            log.debug("SSE 에러: {} - {}", key, e.getMessage());
        });

        emitters.put(key, emitter);
        log.debug("SSE 구독 등록: {}", key);

        // 연결 확인용 초기 이벤트
        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (IOException e) {
            emitters.remove(key);
        }

        return emitter;
    }

    /**
     * 분석 완료/실패 시 해당 predictionId를 구독 중인 클라이언트에 이벤트를 전송합니다.
     */
    public void notifyAnalysisComplete(String username, Long predictionId, Object payload) {
        String key = buildKey(username, predictionId);
        SseEmitter emitter = emitters.remove(key);
        if (emitter == null) {
            log.debug("SSE 수신자 없음 (이미 닫혔거나 폴링 방식): {}", key);
            return;
        }

        try {
            emitter.send(SseEmitter.event().name("analysis-complete").data(payload));
            emitter.complete();
            log.info("SSE 분석 완료 이벤트 전송: {}", key);
        } catch (IOException e) {
            log.warn("SSE 전송 실패: {} - {}", key, e.getMessage());
            emitter.completeWithError(e);
        }
    }

    private String buildKey(String username, Long predictionId) {
        return username + ":" + predictionId;
    }
}
