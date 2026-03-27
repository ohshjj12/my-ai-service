package com.example.aiservice.controller;

import com.example.aiservice.dto.SignupRequest;
import com.example.aiservice.entity.User;
import com.example.aiservice.security.JwtTokenProvider;
import com.example.aiservice.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private UserService userService;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private AuthenticationManager authenticationManager;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void signup_success_returns201() throws Exception {
        SignupRequest request = new SignupRequest();
        request.setUsername("alice");
        request.setPassword("Passw0rd!!");
        request.setEmail("alice@example.com");

        User savedUser = new User();
        savedUser.setUsername("alice");
        when(userService.registerUser(any(SignupRequest.class))).thenReturn(savedUser);

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.username").value("alice"))
                .andExpect(jsonPath("$.message").value("회원가입 성공"));
    }

    @Test
    void signup_invalidRequest_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
