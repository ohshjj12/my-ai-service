package com.example.aiservice.repository;

import com.example.aiservice.entity.Pregnancy;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PregnancyRepository extends JpaRepository<Pregnancy, Long> {
    List<Pregnancy> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<Pregnancy> findByIdAndUserId(Long id, Long userId);
}
