package com.example.aiservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

@Data
public class PregnancyRequest {

    @NotBlank(message = "태명을 입력해주세요")
    private String nickname;

    @NotNull(message = "출산 예정일을 입력해주세요")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dueDate;
}
