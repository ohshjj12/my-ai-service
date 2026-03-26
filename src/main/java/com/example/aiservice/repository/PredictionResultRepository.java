package com.example.aiservice.repository;

import com.example.aiservice.entity.PredictionResult;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PredictionResultRepository extends JpaRepository<PredictionResult, Long> {
    List<PredictionResult> findByUltrasoundImageUserId(Long userId);
}
