package com.example.aiservice.service;

import com.example.aiservice.dto.PregnancyRequest;
import com.example.aiservice.dto.PregnancyResponse;
import com.example.aiservice.dto.TimelineResponse;
import com.example.aiservice.entity.Pregnancy;
import com.example.aiservice.entity.UltrasoundImage;
import com.example.aiservice.entity.User;
import com.example.aiservice.repository.PregnancyRepository;
import com.example.aiservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 타임라인 조회 서비스 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
class PregnancyServiceTest {

    @Mock private PregnancyRepository pregnancyRepository;
    @Mock private UserRepository userRepository;
    @Mock private WeeklyBabyInfoProvider weeklyBabyInfoProvider;

    @InjectMocks
    private PregnancyService pregnancyService;

    private User mockUser;
    private Pregnancy mockPregnancy;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("alice");

        mockPregnancy = new Pregnancy();
        mockPregnancy.setId(10L);
        mockPregnancy.setNickname("우리 아기");
        mockPregnancy.setDueDate(LocalDate.of(2026, 9, 1));
        mockPregnancy.setUser(mockUser);
        mockPregnancy.setCreatedAt(LocalDateTime.now());
        mockPregnancy.setUltrasoundImages(new ArrayList<>());
    }

    // ── create ────────────────────────────────────────────────

    @Test
    void create_validRequest_returnsPregnancyResponse() {
        PregnancyRequest request = new PregnancyRequest();
        request.setNickname("우리 아기");
        request.setDueDate(LocalDate.of(2026, 9, 1));

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(mockUser));
        when(pregnancyRepository.save(any(Pregnancy.class))).thenReturn(mockPregnancy);

        PregnancyResponse response = pregnancyService.create("alice", request);

        assertThat(response.getNickname()).isEqualTo("우리 아기");
        assertThat(response.getDueDate()).isEqualTo(LocalDate.of(2026, 9, 1));
        verify(pregnancyRepository).save(any());
    }

    @Test
    void create_unknownUser_throwsException() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        PregnancyRequest req = new PregnancyRequest();
        req.setNickname("test");
        req.setDueDate(LocalDate.now());

        assertThatThrownBy(() -> pregnancyService.create("unknown", req))
                .isInstanceOf(org.springframework.security.core.userdetails.UsernameNotFoundException.class);
    }

    // ── findAll ───────────────────────────────────────────────

    @Test
    void findAll_returnsAllPregnanciesForUser() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(mockUser));
        when(pregnancyRepository.findByUserIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(mockPregnancy));

        List<PregnancyResponse> result = pregnancyService.findAll("alice");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getNickname()).isEqualTo("우리 아기");
    }

    @Test
    void findAll_noPregnancies_returnsEmptyList() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(mockUser));
        when(pregnancyRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());

        List<PregnancyResponse> result = pregnancyService.findAll("alice");

        assertThat(result).isEmpty();
    }

    // ── getTimeline ───────────────────────────────────────────

    @Test
    void getTimeline_noImages_returnsEmptyItems() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(mockUser));
        when(pregnancyRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(mockPregnancy));

        TimelineResponse timeline = pregnancyService.getTimeline("alice", 10L);

        assertThat(timeline.getPregnancyId()).isEqualTo(10L);
        assertThat(timeline.getNickname()).isEqualTo("우리 아기");
        assertThat(timeline.getItems()).isEmpty();
        assertThat(timeline.getCurrentWeekInfo()).isNull(); // 주차 정보 없음
    }

    @Test
    void getTimeline_withImages_returnsItems() {
        UltrasoundImage image = new UltrasoundImage();
        image.setId(100L);
        image.setOriginalFileName("scan.jpg");
        image.setWeekNumber(20);
        image.setUploadedAt(LocalDateTime.now());
        image.setUser(mockUser);

        mockPregnancy.getUltrasoundImages().add(image);

        com.example.aiservice.dto.WeeklyBabyInfo weekInfo =
                com.example.aiservice.dto.WeeklyBabyInfo.builder()
                        .week(20).title("20주").description("움직임").lengthCm(25.7).weightG(300).build();

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(mockUser));
        when(pregnancyRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(mockPregnancy));
        when(weeklyBabyInfoProvider.getInfoOrNearest(20)).thenReturn(weekInfo);

        TimelineResponse timeline = pregnancyService.getTimeline("alice", 10L);

        assertThat(timeline.getItems()).hasSize(1);
        assertThat(timeline.getItems().get(0).getWeekNumber()).isEqualTo(20);
        assertThat(timeline.getCurrentWeekInfo()).isNotNull();
        assertThat(timeline.getCurrentWeekInfo().getWeek()).isEqualTo(20);
    }

    @Test
    void getTimeline_wrongUser_throwsException() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(mockUser));
        when(pregnancyRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pregnancyService.getTimeline("alice", 10L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("임신 정보를 찾을 수 없습니다");
    }

    // ── totalImages ───────────────────────────────────────────

    @Test
    void findAll_totalImages_countCorrectly() {
        UltrasoundImage img1 = new UltrasoundImage();
        UltrasoundImage img2 = new UltrasoundImage();
        mockPregnancy.getUltrasoundImages().add(img1);
        mockPregnancy.getUltrasoundImages().add(img2);

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(mockUser));
        when(pregnancyRepository.findByUserIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(mockPregnancy));

        List<PregnancyResponse> result = pregnancyService.findAll("alice");

        assertThat(result.get(0).getTotalImages()).isEqualTo(2);
    }
}
