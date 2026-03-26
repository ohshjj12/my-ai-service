package com.example.aiservice.repository;

import com.example.aiservice.entity.UltrasoundImage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UltrasoundImageRepository extends JpaRepository<UltrasoundImage, Long> {
    List<UltrasoundImage> findByUserId(Long userId);
}
