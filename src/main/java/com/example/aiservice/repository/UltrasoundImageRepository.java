package com.example.aiservice.repository;

import com.example.aiservice.entity.UltrasoundImage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UltrasoundImageRepository extends JpaRepository<UltrasoundImage, Long> {
    Optional<UltrasoundImage> findByIdAndUserId(Long id, Long userId);
}
