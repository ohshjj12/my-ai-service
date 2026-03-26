package com.example.aiservice.controller;

import com.example.aiservice.dto.PredictionResponse;
import com.example.aiservice.service.PredictionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/predictions")
@RequiredArgsConstructor
public class PredictionController {

    private final PredictionService predictionService;

    @PostMapping("/analyze")
    public ResponseEntity<PredictionResponse> analyzeImage(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) throws IOException, InterruptedException, ExecutionException {
        PredictionResponse response = predictionService.analyzeImage(file, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my-results")
    public ResponseEntity<List<PredictionResponse>> getMyResults(Authentication authentication) {
        List<PredictionResponse> results = predictionService.getResultsByUser(authentication.getName());
        return ResponseEntity.ok(results);
    }
}
