package com.auction.auth.service;

import com.auction.auth.dto.AuthResponse;
import com.auction.auth.dto.LoginRequest;
import com.auction.auth.dto.RegisterRequest;
import com.auction.auth.dto.ValidateTokenResponse;
import com.auction.auth.entity.Role;
import com.auction.auth.entity.User;
import com.auction.auth.exception.InvalidCredentialsException;
import com.auction.auth.exception.UserAlreadyExistsException;
import com.auction.auth.repository.UserRepository;
import com.auction.auth.security.JwtTokenProvider;
import com.auction.auth.service.impl.AuthServiceImpl;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthServiceImpl authService;

    private User sampleUser;
    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1L)
                .username("john_buyer")
                .email("john@example.com")
                .password("encoded_password")
                .role(Role.BUYER)
                .build();

        registerRequest = RegisterRequest.builder()
                .username("john_buyer")
                .email("john@example.com")
                .password("password123")
                .role(Role.BUYER)
                .build();

        loginRequest = LoginRequest.builder()
                .email("john@example.com")
                .password("password123")
                .build();
    }

    @Test
    void testRegisterSuccess() {
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("john_buyer")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(jwtTokenProvider.generateToken(1L, "john@example.com", "john_buyer", Role.BUYER)).thenReturn("mock.jwt.token");
        when(jwtTokenProvider.getExpirationMs()).thenReturn(86400000L);

        AuthResponse response = authService.register(registerRequest);

        assertNotNull(response);
        assertEquals("mock.jwt.token", response.getToken());
        assertEquals("john@example.com", response.getEmail());
        assertEquals(Role.BUYER, response.getRole());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testRegisterDuplicateEmailThrowsException() {
        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> authService.register(registerRequest));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testLoginSuccess() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("password123", "encoded_password")).thenReturn(true);
        when(jwtTokenProvider.generateToken(1L, "john@example.com", "john_buyer", Role.BUYER)).thenReturn("mock.jwt.token");
        when(jwtTokenProvider.getExpirationMs()).thenReturn(86400000L);

        AuthResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("mock.jwt.token", response.getToken());
        assertEquals(1L, response.getUserId());
    }

    @Test
    void testLoginWrongPasswordThrowsException() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("password123", "encoded_password")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> authService.login(loginRequest));
    }

    @Test
    void testValidateTokenValid() {
        when(jwtTokenProvider.validateToken("mock.jwt.token")).thenReturn(true);
        Claims claims = mock(Claims.class);
        when(claims.get("userId", Long.class)).thenReturn(1L);
        when(claims.getSubject()).thenReturn("john@example.com");
        when(claims.get("role", String.class)).thenReturn("BUYER");
        when(jwtTokenProvider.extractAllClaims("mock.jwt.token")).thenReturn(claims);

        ValidateTokenResponse response = authService.validateToken("Bearer mock.jwt.token");

        assertTrue(response.isValid());
        assertEquals(1L, response.getUserId());
        assertEquals("john@example.com", response.getEmail());
    }

    @Test
    void testValidateTokenInvalid() {
        when(jwtTokenProvider.validateToken("invalid.token")).thenReturn(false);

        ValidateTokenResponse response = authService.validateToken("invalid.token");

        assertFalse(response.isValid());
    }
}
