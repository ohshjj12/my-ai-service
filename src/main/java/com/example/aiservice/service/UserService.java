package com.example.aiservice.service;

import com.example.aiservice.dto.SignupRequest;
import com.example.aiservice.entity.RefreshToken;
import com.example.aiservice.entity.User;
import com.example.aiservice.repository.RefreshTokenRepository;
import com.example.aiservice.repository.UserRepository;
import com.example.aiservice.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;

    public User registerUser(SignupRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already taken: " + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered: " + request.getEmail());
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        return userRepository.save(user);
    }

    /**
     * Refresh Token 새로 생성 후 DB 저장, 토큰 값 반환
     */
    @Transactional
    public String createRefreshToken(String username) {
        // 기존 토큰 파기
        refreshTokenRepository.deleteByUsername(username);
        String tokenValue = jwtTokenProvider.generateRefreshToken();
        RefreshToken entity = new RefreshToken(
                tokenValue,
                username,
                jwtTokenProvider.refreshTokenExpiresAt()
        );
        refreshTokenRepository.save(entity);
        return tokenValue;
    }

    /**
     * Refresh Token 검증 후 Access Token 재발급
     */
    @Transactional
    public String refreshAccessToken(String rawRefreshToken) {
        RefreshToken entity = refreshTokenRepository.findByToken(rawRefreshToken)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 Refresh Token입니다"));
        if (entity.isExpired()) {
            refreshTokenRepository.delete(entity);
            throw new IllegalArgumentException("만료된 Refresh Token입니다. 다시 로그인 해주세요");
        }
        return jwtTokenProvider.generateToken(entity.getUsername());
    }

    /**
     * 로그아웃: Refresh Token 삭제
     */
    @Transactional
    public void logout(String username) {
        refreshTokenRepository.deleteByUsername(username);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                List.of(new SimpleGrantedAuthority(user.getRole()))
        );
    }
}
