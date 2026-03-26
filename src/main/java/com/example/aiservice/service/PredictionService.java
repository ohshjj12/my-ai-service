package com.example.aiservice.service;

import com.example.aiservice.dto.PredictionResponse;
import com.example.aiservice.entity.PredictionResult;
import com.example.aiservice.entity.UltrasoundImage;
import com.example.aiservice.entity.User;
import com.example.aiservice.repository.PredictionResultRepository;
import com.example.aiservice.repository.UltrasoundImageRepository;
import com.example.aiservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PredictionService {

    private final UltrasoundImageRepository ultrasoundImageRepository;
    private final PredictionResultRepository predictionResultRepository;
    private final UserRepository userRepository;

    public PredictionResponse analyzeImage(MultipartFile file, String username) throws IOException, InterruptedException, ExecutionException {
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Only image files are accepted.");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        UltrasoundImage image = new UltrasoundImage();
        image.setOriginalFileName(file.getOriginalFilename());
        image.setContentType(contentType);
        image.setFileSize(file.getSize());
        image.setUser(user);
        UltrasoundImage savedImage = ultrasoundImageRepository.save(image);

        // Run AI analysis on a Virtual Thread
        try (ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<PredictionResult> future = virtualExecutor.submit(() -> {
                log.info("Starting AI analysis for image '{}' on thread: {}", file.getOriginalFilename(), Thread.currentThread());
                // Simulate a 3-second AI analysis delay
                Thread.sleep(3000);
                return performPrediction(savedImage);
            });

            PredictionResult result = future.get();
            log.info("AI analysis complete: gender={}, confidence={}", result.getPredictedGender(), result.getConfidenceScore());

            return mapToResponse(result);
        }
    }

    private PredictionResult performPrediction(UltrasoundImage image) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        String gender = random.nextBoolean() ? "MALE" : "FEMALE";
        double confidence = 0.60 + (random.nextDouble() * 0.39); // 60% - 99%

        PredictionResult result = new PredictionResult();
        result.setPredictedGender(gender);
        result.setConfidenceScore(Math.round(confidence * 10000.0) / 10000.0);
        result.setUltrasoundImage(image);
        return predictionResultRepository.save(result);
    }

    public List<PredictionResponse> getResultsByUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        return predictionResultRepository.findByUltrasoundImageUserId(user.getId())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private PredictionResponse mapToResponse(PredictionResult result) {
        return PredictionResponse.builder()
                .predictionId(result.getId())
                .imageId(result.getUltrasoundImage().getId())
                .originalFileName(result.getUltrasoundImage().getOriginalFileName())
                .predictedGender(result.getPredictedGender())
                .confidenceScore(result.getConfidenceScore())
                .analyzedAt(result.getAnalyzedAt())
                .uploadedBy(result.getUltrasoundImage().getUser().getUsername())
                .build();
    }
}
