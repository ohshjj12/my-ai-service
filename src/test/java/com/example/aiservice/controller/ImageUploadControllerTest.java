package com.example.aiservice.controller;

import com.example.aiservice.dto.PredictionResponse;
import com.example.aiservice.security.JwtAuthenticationFilter;
import com.example.aiservice.security.JwtTokenProvider;
import com.example.aiservice.service.AnalysisQueueService;
import com.example.aiservice.service.PredictionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 파일 업로드 API 통합 테스트
 * POST /api/predictions/analyze
 */
@WebMvcTest(PredictionController.class)
@AutoConfigureMockMvc(addFilters = false)
class ImageUploadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private PredictionService predictionService;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private JwtAuthenticationFilter jwtAuthenticationFilter;

    private PredictionResponse dummyResponse(String status) {
        return PredictionResponse.builder()
                .predictionId(1L)
                .imageId(10L)
                .originalFileName("scan.jpg")
                .predictedGender("MALE")
                .confidenceScore(0.87)
                .analysisStatus(status)
                .reportText("건강한 남아입니다!")
                .analyzedAt(LocalDateTime.now())
                .uploadedBy("alice")
                .build();
    }

    @Test
    @WithMockUser(username = "alice")
    void analyze_validJpeg_returns202() throws Exception {
        // JPEG 매직 바이트로 시작하는 파일
        byte[] jpegBytes = {(byte)0xFF, (byte)0xD8, (byte)0xFF, 0x00, 0x01, 0x02, 0x03};
        MockMultipartFile file = new MockMultipartFile(
                "file", "scan.jpg", "image/jpeg", jpegBytes);

        when(predictionService.enqueueAnalysis(any(), eq("alice"), isNull(), isNull()))
                .thenReturn(dummyResponse("PENDING"));

        mockMvc.perform(multipart("/api/predictions/analyze").file(file))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.originalFileName").value("scan.jpg"));
    }

    @Test
    @WithMockUser(username = "alice")
    void analyze_withWeekNumber_passesWeekToService() throws Exception {
        byte[] jpegBytes = {(byte)0xFF, (byte)0xD8, (byte)0xFF, 0x00};
        MockMultipartFile file = new MockMultipartFile(
                "file", "scan.jpg", "image/jpeg", jpegBytes);

        when(predictionService.enqueueAnalysis(any(), eq("alice"), isNull(), eq(20)))
                .thenReturn(dummyResponse("DONE"));

        mockMvc.perform(multipart("/api/predictions/analyze")
                        .file(file)
                        .param("weekNumber", "20"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.analysisStatus").value("DONE"));
    }

    @Test
    @WithMockUser(username = "alice")
    void analyze_noFile_returns400() throws Exception {
        mockMvc.perform(multipart("/api/predictions/analyze"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "alice")
    void getStatus_existingId_returnsResult() throws Exception {
        when(predictionService.getResultById(1L, "alice"))
                .thenReturn(dummyResponse("DONE"));

        mockMvc.perform(get("/api/predictions/1/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.analysisStatus").value("DONE"))
                .andExpect(jsonPath("$.data.predictedGender").value("MALE"));
    }

    @Test
    @WithMockUser(username = "alice")
    void getStatus_notFound_returns404or400() throws Exception {
        when(predictionService.getResultById(eq(999L), any()))
                .thenThrow(new IllegalArgumentException("결과를 찾을 수 없습니다: 999"));

        mockMvc.perform(get("/api/predictions/999/status"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "alice")
    void getMyResults_returnsListWrappedInApiResponse() throws Exception {
        when(predictionService.getResultsByUser("alice"))
                .thenReturn(java.util.List.of(dummyResponse("DONE")));

        mockMvc.perform(get("/api/predictions/my-results"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].predictionId").value(1));
    }
}
