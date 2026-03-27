package com.example.aiservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SignupRequest {
    @NotBlank(message = "사용자명을 입력해주세요")
    @Size(min = 3, max = 50, message = "사용자명은 3~50자 사이여야 합니다")
    private String username;

    @NotBlank(message = "비밀번호를 입력해주세요")
    @Size(min = 6, max = 100, message = "비밀번호는 6자 이상이어야 합니다")
    private String password;

    @NotBlank(message = "이메일을 입력해주세요")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    private String email;
}
