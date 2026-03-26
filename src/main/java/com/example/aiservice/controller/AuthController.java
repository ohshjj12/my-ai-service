package com.example.aiservice.controller;

import com.example.aiservice.dto.AuthResponse;
import com.example.aiservice.dto.SignupRequest;
import com.example.aiservice.entity.User;
import com.example.aiservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest request) {
        User user = userService.registerUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AuthResponse("User registered successfully", user.getUsername()));
    }
}
