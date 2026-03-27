package com.example.aiservice.service;

import com.example.aiservice.dto.PregnancyRequest;
import com.example.aiservice.dto.PregnancyResponse;
import com.example.aiservice.dto.TimelineResponse;
import com.example.aiservice.dto.WeeklyBabyInfo;
import com.example.aiservice.entity.Pregnancy;
import com.example.aiservice.entity.PredictionResult;
import com.example.aiservice.entity.UltrasoundImage;
import com.example.aiservice.entity.User;
import com.example.aiservice.repository.PregnancyRepository;
import com.example.aiservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PregnancyService {

    private final PregnancyRepository pregnancyRepository;
    private final UserRepository userRepository;
    private final WeeklyBabyInfoProvider weeklyBabyInfoProvider;

    @Transactional
    public PregnancyResponse create(String username, PregnancyRequest request) {
        User user = findUser(username);
        Pregnancy pregnancy = new Pregnancy();
        pregnancy.setNickname(request.getNickname());
        pregnancy.setDueDate(request.getDueDate());
        pregnancy.setUser(user);
        Pregnancy saved = pregnancyRepository.save(pregnancy);
        return toResponse(saved);
    }

    public List<PregnancyResponse> findAll(String username) {
        User user = findUser(username);
        return pregnancyRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public TimelineResponse getTimeline(String username, Long pregnancyId) {
        User user = findUser(username);
        Pregnancy pregnancy = pregnancyRepository.findByIdAndUserId(pregnancyId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("임신 정보를 찾을 수 없습니다: " + pregnancyId));

        List<TimelineResponse.TimelineItemResponse> items = pregnancy.getUltrasoundImages()
                .stream()
                .map(this::toTimelineItem)
                .collect(Collectors.toList());

        // 가장 최근 주차 기준으로 현재 아기 표준 정보 계산
        int latestWeek = pregnancy.getUltrasoundImages().stream()
                .map(UltrasoundImage::getWeekNumber)
                .filter(w -> w != null)
                .max(Comparator.naturalOrder())
                .orElse(0);
        WeeklyBabyInfo currentWeekInfo = latestWeek > 0
                ? weeklyBabyInfoProvider.getInfoOrNearest(latestWeek)
                : null;

        return TimelineResponse.builder()
                .pregnancyId(pregnancy.getId())
                .nickname(pregnancy.getNickname())
                .currentWeekInfo(currentWeekInfo)
                .items(items)
                .build();
    }

    private TimelineResponse.TimelineItemResponse toTimelineItem(UltrasoundImage image) {
        PredictionResult result = image.getPredictionResult();
        TimelineResponse.TimelineItemResponse.TimelineItemResponseBuilder builder =
                TimelineResponse.TimelineItemResponse.builder()
                        .imageId(image.getId())
                        .originalFileName(image.getOriginalFileName())
                        .weekNumber(image.getWeekNumber())
                        .uploadedAt(image.getUploadedAt());

        if (result != null) {
            builder
                    .predictionId(result.getId())
                    .predictedGender(result.getPredictedGender())
                    .confidenceScore(result.getConfidenceScore())
                    .analysisStatus(result.getAnalysisStatus() != null ? result.getAnalysisStatus().name() : null)
                    .reportText(result.getReportText())
                    .analyzedAt(result.getAnalyzedAt());
        }

        // 주차 정보가 있으면 해당 주차 아기 표준 정보 포함
        if (image.getWeekNumber() != null) {
            builder.babyInfo(weeklyBabyInfoProvider.getInfoOrNearest(image.getWeekNumber()));
        }
        return builder.build();
    }

    private PregnancyResponse toResponse(Pregnancy p) {
        int totalImages = (p.getUltrasoundImages() != null) ? p.getUltrasoundImages().size() : 0;
        return PregnancyResponse.builder()
                .id(p.getId())
                .nickname(p.getNickname())
                .dueDate(p.getDueDate())
                .createdAt(p.getCreatedAt())
                .totalImages(totalImages)
                .build();
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }
}
