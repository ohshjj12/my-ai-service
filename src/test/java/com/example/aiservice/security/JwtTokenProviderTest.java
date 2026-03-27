package com.example.aiservice.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider provider;

    private static final String SECRET =
            "test-jwt-secret-key-for-unit-tests-must-be-256bits!!";
    private static final long EXPIRATION         = 3600000L;   // 1h
    private static final long REFRESH_EXPIRATION = 2592000000L; // 30d

    @BeforeEach
    void setUp() {
        provider = new JwtTokenProvider();
        ReflectionTestUtils.setField(provider, "secret", SECRET);
        ReflectionTestUtils.setField(provider, "expiration", EXPIRATION);
        ReflectionTestUtils.setField(provider, "refreshExpiration", REFRESH_EXPIRATION);
    }

    @Test
    void generateToken_and_extractUsername() {
        String token = provider.generateToken("alice");
        assertThat(token).isNotBlank();
        assertThat(provider.extractUsername(token)).isEqualTo("alice");
    }

    @Test
    void isTokenValid_validToken_returnsTrue() {
        String token = provider.generateToken("alice");
        assertThat(provider.isTokenValid(token)).isTrue();
    }

    @Test
    void isTokenValid_invalidToken_returnsFalse() {
        assertThat(provider.isTokenValid("invalid.token.value")).isFalse();
    }

    @Test
    void isTokenValid_expiredToken_returnsFalse() {
        // 만료 시간 0으로 설정 후 토큰 생성
        ReflectionTestUtils.setField(provider, "expiration", 0L);
        String expiredToken = provider.generateToken("alice");
        // 즉시 만료됐으므로 valid = false
        assertThat(provider.isTokenValid(expiredToken)).isFalse();
    }

    @Test
    void generateRefreshToken_isUUID() {
        String rt = provider.generateRefreshToken();
        assertThat(rt).hasSize(32); // UUID without hyphens
    }

    @Test
    void refreshTokenExpiresAt_isInFuture() {
        var expiresAt = provider.refreshTokenExpiresAt();
        assertThat(expiresAt).isAfter(java.time.LocalDateTime.now());
    }
}
