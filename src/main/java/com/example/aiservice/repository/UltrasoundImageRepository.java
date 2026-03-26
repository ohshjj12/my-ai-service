package com.example.aiservice.repository;

import com.example.aiservice.entity.UltrasoundImage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UltrasoundImageRepository extends JpaRepository<UltrasoundImage, Long> {
}
