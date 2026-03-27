package com.example.aiservice.repository;

import com.example.aiservice.entity.ApiUsageLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface ApiUsageLogRepository extends JpaRepository<ApiUsageLog, Long> {

    List<ApiUsageLog> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime from, LocalDateTime to);

    @Query("SELECT SUM(a.estimatedCostUsd) FROM ApiUsageLog a WHERE a.createdAt >= :from")
    BigDecimal sumCostSince(LocalDateTime from);

    @Query("SELECT SUM(a.totalTokens) FROM ApiUsageLog a WHERE a.createdAt >= :from")
    Long sumTokensSince(LocalDateTime from);
}
