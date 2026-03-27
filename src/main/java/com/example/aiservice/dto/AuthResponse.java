package com.example.aiservice.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthResponse {
    private String message;
    private String username;
    private String accessToken;
    private String tokenType;
    private String refreshToken;
}
