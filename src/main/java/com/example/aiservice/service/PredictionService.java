package com.example.aiservice.service;

import com.example.aiservice.client.OpenAiVisionClient;
import com.example.aiservice.client.dto.AiAnalysisResult;
import com.example.aiservice.dto.PredictionResponse;
import com.example.aiservice.entity.AnalysisStatus;
import com.example.aiservice.entity.Pregnancy;
import com.example.aiservice.entity.PredictionResult;
import com.example.aiservice.entity.UltrasoundImage;
import com.example.aiservice.entity.User;
import com.example.aiservice.repository.PregnancyRepository;
import com.example.aiservice.repository.PredictionResultRepository;
import com.example.aiservice.repository.UltrasoundImageRepository;
import com.example.aiservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PredictionService {

    private final UltrasoundImageRepository ultrasoundImageRepository;
    private final PredictionResultRepository predictionResultRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final OpenAiVisionClient openAiVisionClient;
    private final PregnancyRepository pregnancyRepository;
    private final AnalysisQueueService analysisQueueService;

    /**
     * 이미지를 저장하고 분석 요청을 큐에 등록합니다.
     * AI 분석은 백그라운드 워커가 처리하므로 즉시 PENDING 상태를 반환합니다.
     * → Controller에서 202 Accepted로 응답
     */
    @Transactional
    public PredictionResponse enqueueAnalysis(MultipartFile file, String username,
                                               Long pregnancyId, Integer weekNumber) throws IOException {
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("이미지 파일만 업로드 가능합니다.");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        byte[] imageBytes = file.getBytes();
        String storedPath = fileStorageService.store(imageBytes, file.getOriginalFilename());

        UltrasoundImage image = new UltrasoundImage();
        image.setOriginalFileName(file.getOriginalFilename());
        image.setContentType(contentType);
        image.setFileSize(file.getSize());
        image.setStoredFilePath(storedPath);
        image.setWeekNumber(weekNumber);
        image.setUser(user);

        if (pregnancyId != null) {
            Pregnancy pregnancy = pregnancyRepository.findByIdAndUserId(pregnancyId, user.getId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "임신 정보를 찾을 수 없습니다: " + pregnancyId));
            image.setPregnancy(pregnancy);
        }

        UltrasoundImage savedImage = ultrasoundImageRepository.save(image);

        // PENDING 상태의 결과 레코드 선점 생성
        PredictionResult pendingResult = new PredictionResult();
        pendingResult.setUltrasoundImage(savedImage);
        pendingResult.setAnalysisStatus(AnalysisStatus.PENDING);
        pendingResult.setReportText("분석을 기다리는 중입니다...");
        PredictionResult saved = predictionResultRepository.save(pendingResult);

        // Redis 활성화 시: 큐에 등록 후 워커가 처리
        // Redis 비활성화 시: 즉시 직접 분석 수행
        if (analysisQueueService.isEnabled()) {
            analysisQueueService.enqueue(saved.getId());
            log.info("분석 큐 등록: predictionId={}, file={}", saved.getId(), file.getOriginalFilename());
        } else {
            log.info("Redis 비활성화 - 즉시 분석 수행: predictionId={}, file={}", saved.getId(), file.getOriginalFilename());
            performAnalysis(saved.getId());
        }

        return mapToResponse(predictionResultRepository.findById(saved.getId()).orElse(saved));
    }

    /**
     * 워커가 호출하는 실제 AI 분석 메서드.
     * predictionResultId로 DB 조회 → 분석 수행 → 상태 업데이트.
     */
    @Transactional
    public void performAnalysis(Long predictionResultId) {
        PredictionResult result = predictionResultRepository.findById(predictionResultId).orElse(null);
        if (result == null) {
            log.warn("분석 대상 없음: predictionResultId={}", predictionResultId);
            return;
        }
        if (result.getAnalysisStatus() != AnalysisStatus.PENDING) {
            log.debug("이미 처리된 항목 건너뜀: id={}, status={}", predictionResultId, result.getAnalysisStatus());
            return;
        }

        UltrasoundImage image = result.getUltrasoundImage();
        log.info("AI 분석 시작: predictionId={}, file={}", predictionResultId, image.getOriginalFileName());

        try {
            byte[] imageBytes = Files.readAllBytes(Paths.get(image.getStoredFilePath()));
            AiAnalysisResult aiResult = openAiVisionClient.analyzeUltrasound(
                    imageBytes, image.getContentType(), predictionResultId);

            result.setPredictedGender(aiResult.getPredictedGender());
            result.setConfidenceScore(aiResult.getConfidenceScore());
            result.setReportText(aiResult.getReportText());
            result.setAnalysisStatus(AnalysisStatus.DONE);
            result.setAnalyzedAt(LocalDateTime.now());
            log.info("AI 분석 완료: predictionId={}, gender={}", predictionResultId, aiResult.getPredictedGender());

        } catch (Exception e) {
            log.error("AI 분석 실패: predictionId={}, error={}", predictionResultId, e.getMessage(), e);
            result.setPredictedGender("UNKNOWN");
            result.setConfidenceScore(0.0);
            result.setReportText("AI 분석 중 오류가 발생했습니다. 관리자에게 문의해주세요.");
            result.setAnalysisStatus(AnalysisStatus.FAILED);
            result.setAnalyzedAt(LocalDateTime.now());
        }

        predictionResultRepository.save(result);
    }

    public List<PredictionResponse> getResultsByUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        return predictionResultRepository.findByUltrasoundImageUserId(user.getId())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public PredictionResponse getResultById(Long predictionId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        PredictionResult result = predictionResultRepository.findById(predictionId)
                .orElseThrow(() -> new IllegalArgumentException("분석 결과를 찾을 수 없습니다: " + predictionId));
        if (!result.getUltrasoundImage().getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("접근 권한이 없습니다.");
        }
        return mapToResponse(result);
    }

    private PredictionResponse mapToResponse(PredictionResult result) {
        return PredictionResponse.builder()
                .predictionId(result.getId())
                .imageId(result.getUltrasoundImage().getId())
                .originalFileName(result.getUltrasoundImage().getOriginalFileName())
                .predictedGender(result.getPredictedGender())
                .confidenceScore(result.getConfidenceScore())
                .analysisStatus(result.getAnalysisStatus() != null
                        ? result.getAnalysisStatus().name() : null)
                .reportText(result.getReportText())
                .analyzedAt(result.getAnalyzedAt())
                .uploadedBy(result.getUltrasoundImage().getUser().getUsername())
                .build();
    }
}
