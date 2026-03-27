package com.example.aiservice.service;

import com.example.aiservice.dto.SignupRequest;
import com.example.aiservice.entity.User;
import com.example.aiservice.repository.RefreshTokenRepository;
import com.example.aiservice.repository.UserRepository;
import com.example.aiservice.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private UserService userService;

    private SignupRequest signupRequest;

    @BeforeEach
    void setUp() {
        signupRequest = new SignupRequest();
        signupRequest.setUsername("alice");
        signupRequest.setPassword("secret123");
        signupRequest.setEmail("alice@example.com");
    }

    @Test
    void registerUser_success() {
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("hashed");

        User savedUser = new User();
        savedUser.setId(1L);
        savedUser.setUsername("alice");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        User result = userService.registerUser(signupRequest);

        assertThat(result.getUsername()).isEqualTo("alice");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerUser_duplicateUsername_throwsException() {
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        assertThatThrownBy(() -> userService.registerUser(signupRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Username already taken");
    }

    @Test
    void registerUser_duplicateEmail_throwsException() {
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.registerUser(signupRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email already registered");
    }

    @Test
    void loadUserByUsername_found() {
        User user = new User();
        user.setUsername("alice");
        user.setPassword("hashed");
        user.setRole("ROLE_USER");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        UserDetails details = userService.loadUserByUsername("alice");

        assertThat(details.getUsername()).isEqualTo("alice");
        assertThat(details.getAuthorities()).hasSize(1);
    }

    @Test
    void loadUserByUsername_notFound_throwsException() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.loadUserByUsername("unknown"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void logout_deletesRefreshToken() {
        userService.logout("alice");
        verify(refreshTokenRepository).deleteByUsername("alice");
    }
}
