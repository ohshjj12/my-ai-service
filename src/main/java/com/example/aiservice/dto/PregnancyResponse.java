package com.example.aiservice.dto;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class PregnancyResponse {
    private Long id;
    private String nickname;
    private LocalDate dueDate;
    private LocalDateTime createdAt;
    private int totalImages;
}
