package com.example.aiservice.client;

import com.example.aiservice.client.dto.AiAnalysisResult;
import com.example.aiservice.entity.ApiUsageLog;
import com.example.aiservice.repository.ApiUsageLogRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Component
@Slf4j
public class OpenAiVisionClient {

    @Value("${openai.api-key:}")
    private String apiKey;

    @Value("${openai.model:gpt-4o}")
    private String model;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final ApiUsageLogRepository apiUsageLogRepository;

    private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";

    // gpt-4o 가격 (2025년 기준, USD per token)
    private static final BigDecimal INPUT_PRICE_PER_TOKEN  = new BigDecimal("0.000005");  // $5 / 1M
    private static final BigDecimal OUTPUT_PRICE_PER_TOKEN = new BigDecimal("0.000015"); // $15 / 1M

    public OpenAiVisionClient(@Qualifier("openAiRestClient") RestClient restClient,
                               ObjectMapper objectMapper,
                               ApiUsageLogRepository apiUsageLogRepository) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.apiUsageLogRepository = apiUsageLogRepository;
    }

    /**
     * 초음파 이미지를 OpenAI Vision API (gpt-4o)로 분석합니다.
     * API 키가 없으면 랜덤 시뮬레이션으로 Fallback합니다.
     *
     * @param predictionResultId 비용 로깅용 예측 ID (nullable)
     */
    public AiAnalysisResult analyzeUltrasound(byte[] imageBytes, String contentType,
                                               Long predictionResultId) {
        if (!isApiKeyConfigured()) {
            log.warn("OPENAI_API_KEY 미설정 -> 시뮬레이션 모드로 동작합니다.");
            return simulateFallback();
        }

        try {
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
            String dataUri = "data:" + contentType + ";base64," + base64Image;

            Map<String, Object> imageContent = Map.of(
                    "type", "image_url",
                    "image_url", Map.of("url", dataUri, "detail", "high")
            );
            Map<String, Object> textContent = Map.of(
                    "type", "text",
                    "text", buildPrompt()
            );
            Map<String, Object> userMessage = Map.of(
                    "role", "user",
                    "content", List.of(imageContent, textContent)
            );
            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "messages", List.of(userMessage),
                    "response_format", Map.of("type", "json_object"),
                    "max_tokens", 500
            );

            String rawResponse = restClient.post()
                    .uri(OPENAI_URL)
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            return parseResponseAndLog(rawResponse, predictionResultId);

        } catch (Exception e) {
            log.error("OpenAI Vision API 호출 실패: {}", e.getMessage(), e);
            throw new RuntimeException("AI 분석 요청 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /** 하위 호환 - predictionResultId 없이 호출 가능 */
    public AiAnalysisResult analyzeUltrasound(byte[] imageBytes, String contentType) {
        return analyzeUltrasound(imageBytes, contentType, null);
    }

    private AiAnalysisResult parseResponseAndLog(String rawResponse, Long predictionResultId) throws Exception {
        JsonNode root = objectMapper.readTree(rawResponse);

        JsonNode errorNode = root.path("error");
        if (!errorNode.isMissingNode()) {
            String errorMsg = errorNode.path("message").asText("Unknown OpenAI error");
            throw new RuntimeException("OpenAI API 오류: " + errorMsg);
        }

        String content = root.path("choices").get(0)
                .path("message").path("content").asText();
        log.debug("OpenAI 응답 content: {}", content);

        // 비용 로깅
        JsonNode usage = root.path("usage");
        if (!usage.isMissingNode()) {
            int promptTokens     = usage.path("prompt_tokens").asInt(0);
            int completionTokens = usage.path("completion_tokens").asInt(0);
            int totalTokens      = usage.path("total_tokens").asInt(0);

            BigDecimal cost = INPUT_PRICE_PER_TOKEN.multiply(BigDecimal.valueOf(promptTokens))
                    .add(OUTPUT_PRICE_PER_TOKEN.multiply(BigDecimal.valueOf(completionTokens)))
                    .setScale(6, RoundingMode.HALF_UP);

            apiUsageLogRepository.save(new ApiUsageLog(
                    model, predictionResultId, promptTokens, completionTokens, totalTokens, cost));

            log.info("OpenAI usage: model={}, prompt={}, completion={}, total={}, cost=${}",
                    model, promptTokens, completionTokens, totalTokens, cost);
        }

        return objectMapper.readValue(content, AiAnalysisResult.class);
    }

    private String buildPrompt() {
        return """
                당신은 산부인과 초음파 전문의입니다.
                첨부된 초음파 이미지를 분석하여 태아 성별을 예측해주세요.
                반드시 아래 형식의 JSON 객체만 응답하세요 (다른 텍스트 없이):
                {
                  "predictedGender": "MALE 또는 FEMALE 중 하나",
                  "confidenceScore": 0.60~0.99 사이의 소수점 두 자리 숫자,
                  "reportText": "부모님을 위한 따뜻하고 전문적인 한국어 분석 리포트 2~3문장. 예측 근거와 함께 희망적인 메시지를 포함해주세요."
                }
                """;
    }

    private boolean isApiKeyConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    private AiAnalysisResult simulateFallback() {
        ThreadLocalRandom rand = ThreadLocalRandom.current();
        boolean isMale = rand.nextBoolean();
        double confidence = Math.round((0.65 + rand.nextDouble() * 0.30) * 100.0) / 100.0;
        String gender = isMale ? "MALE" : "FEMALE";
        String report = isMale
                ? String.format("초음파 이미지 분석 결과, 남아일 가능성이 %.0f%%로 예측됩니다. "
                  + "생식기 부위의 특징적인 형태가 관찰되었습니다. "
                  + "건강한 아기의 탄생을 기원드립니다! (시뮬레이션 결과)", confidence * 100)
                : String.format("초음파 이미지 분석 결과, 여아일 가능성이 %.0f%%로 예측됩니다. "
                  + "생식기 부위의 3개의 선(three lines sign)이 관찰되었습니다. "
                  + "건강하고 예쁜 아기의 탄생을 기원드립니다! (시뮬레이션 결과)", confidence * 100);
        return new AiAnalysisResult(gender, confidence, report);
    }
}
