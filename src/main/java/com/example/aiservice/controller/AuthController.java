package com.example.aiservice.controller;

import com.example.aiservice.dto.ApiResponse;
import com.example.aiservice.dto.AuthResponse;
import com.example.aiservice.dto.LoginRequest;
import com.example.aiservice.dto.SignupRequest;
import com.example.aiservice.dto.TokenRefreshRequest;
import com.example.aiservice.entity.User;
import com.example.aiservice.security.JwtTokenProvider;
import com.example.aiservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<AuthResponse>> signup(@Valid @RequestBody SignupRequest request) {
        User user = userService.registerUser(request);
        AuthResponse body = AuthResponse.builder()
                .message("회원가입 성공")
                .username(user.getUsername())
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("회원가입 성공", body));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        String accessToken = jwtTokenProvider.generateToken(auth.getName());
        String refreshToken = userService.createRefreshToken(auth.getName());

        AuthResponse body = AuthResponse.builder()
                .message("로그인 성공")
                .username(auth.getName())
                .accessToken(accessToken)
                .tokenType("Bearer")
                .refreshToken(refreshToken)
                .build();
        return ResponseEntity.ok(ApiResponse.success("로그인 성공", body));
    }

    /** Access Token 재발급 */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody TokenRefreshRequest request) {
        String newAccessToken = userService.refreshAccessToken(request.getRefreshToken());
        AuthResponse body = AuthResponse.builder()
                .message("Access Token 재발급 성공")
                .accessToken(newAccessToken)
                .tokenType("Bearer")
                .build();
        return ResponseEntity.ok(ApiResponse.success("Access Token 재발급 성공", body));
    }

    /** 로그아웃: Refresh Token 삭제 */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@AuthenticationPrincipal UserDetails userDetails) {
        userService.logout(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("로그아웃 성공", null));
    }
}
