package com.example.aiservice.service;

import com.example.aiservice.client.OpenAiVisionClient;
import com.example.aiservice.client.dto.AiAnalysisResult;
import com.example.aiservice.entity.*;
import com.example.aiservice.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PredictionServiceTest {

    @Mock private UltrasoundImageRepository ultrasoundImageRepository;
    @Mock private PredictionResultRepository predictionResultRepository;
    @Mock private UserRepository userRepository;
    @Mock private FileStorageService fileStorageService;
    @Mock private OpenAiVisionClient openAiVisionClient;
    @Mock private PregnancyRepository pregnancyRepository;
    @Mock private AnalysisQueueService analysisQueueService;

    @InjectMocks
    private PredictionService predictionService;

    private User mockUser;
    private UltrasoundImage mockImage;
    private PredictionResult mockResult;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("alice");

        mockImage = new UltrasoundImage();
        mockImage.setId(10L);
        mockImage.setOriginalFileName("test.jpg");
        mockImage.setContentType("image/jpeg");
        mockImage.setStoredFilePath("/tmp/test.jpg");
        mockImage.setUser(mockUser);

        mockResult = new PredictionResult();
        mockResult.setId(100L);
        mockResult.setUltrasoundImage(mockImage);
        mockResult.setAnalysisStatus(AnalysisStatus.PENDING);
        mockResult.setReportText("분석을 기다리는 중입니다...");
    }

    @Test
    void performAnalysis_success_updatesToDone() {
        // PENDING 상태 결과 반환
        when(predictionResultRepository.findById(100L)).thenReturn(Optional.of(mockResult));
        // 파일 읽기 시 예외 처리 → storedFilePath를 임시 파일로
        // AI 클라이언트 Mock
        AiAnalysisResult aiResult = new AiAnalysisResult("MALE", 0.85, "건강한 남아입니다!");
        try {
            // storedFilePath 존재하지 않으므로 IOException 발생 → FAILED로 처리되는 흐름 테스트
            // 실제 파일 없이 테스트하려면 spy로 오버라이드하거나 경로를 존재하는 파일로 설정
            java.nio.file.Path tmpFile = java.nio.file.Files.createTempFile("test", ".jpg");
            java.nio.file.Files.write(tmpFile, new byte[]{(byte)0xFF, (byte)0xD8, (byte)0xFF});
            mockImage.setStoredFilePath(tmpFile.toAbsolutePath().toString());

            when(openAiVisionClient.analyzeUltrasound(any(), anyString(), anyLong()))
                    .thenReturn(aiResult);
            when(predictionResultRepository.save(any())).thenReturn(mockResult);

            predictionService.performAnalysis(100L);

            verify(predictionResultRepository).save(argThat(r ->
                    r.getAnalysisStatus() == AnalysisStatus.DONE &&
                    "MALE".equals(r.getPredictedGender())));
        } catch (Exception e) {
            fail("예외 발생: " + e.getMessage());
        }
    }

    @Test
    void performAnalysis_aiFailure_updatesToFailed() throws Exception {
        when(predictionResultRepository.findById(100L)).thenReturn(Optional.of(mockResult));

        java.nio.file.Path tmpFile = java.nio.file.Files.createTempFile("test", ".jpg");
        java.nio.file.Files.write(tmpFile, new byte[]{(byte)0xFF, (byte)0xD8, (byte)0xFF});
        mockImage.setStoredFilePath(tmpFile.toAbsolutePath().toString());

        when(openAiVisionClient.analyzeUltrasound(any(), anyString(), anyLong()))
                .thenThrow(new RuntimeException("API 오류"));
        when(predictionResultRepository.save(any())).thenReturn(mockResult);

        predictionService.performAnalysis(100L);

        verify(predictionResultRepository).save(argThat(r ->
                r.getAnalysisStatus() == AnalysisStatus.FAILED));
    }

    @Test
    void performAnalysis_alreadyDone_skips() {
        mockResult.setAnalysisStatus(AnalysisStatus.DONE);
        when(predictionResultRepository.findById(100L)).thenReturn(Optional.of(mockResult));

        predictionService.performAnalysis(100L);

        verify(openAiVisionClient, never()).analyzeUltrasound(any(), any(), any());
    }

    @Test
    void performAnalysis_notFound_doesNothing() {
        when(predictionResultRepository.findById(999L)).thenReturn(Optional.empty());

        predictionService.performAnalysis(999L);

        verify(openAiVisionClient, never()).analyzeUltrasound(any(), any(), any());
    }

    @Test
    void getResultById_wrongUser_throwsException() {
        User otherUser = new User();
        otherUser.setId(2L);
        otherUser.setUsername("bob");
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(otherUser));
        when(predictionResultRepository.findById(100L)).thenReturn(Optional.of(mockResult));

        assertThatThrownBy(() -> predictionService.getResultById(100L, "bob"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("접근 권한");
    }
}
