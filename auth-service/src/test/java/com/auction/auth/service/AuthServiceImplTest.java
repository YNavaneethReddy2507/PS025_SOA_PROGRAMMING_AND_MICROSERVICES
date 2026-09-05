package com.auction.auth.service;

import com.auction.auth.dto.AuthResponse;
import com.auction.auth.dto.LoginRequest;
import com.auction.auth.dto.RegisterRequest;
import com.auction.auth.dto.ValidateTokenResponse;
import com.auction.auth.entity.Role;
import com.auction.auth.entity.User;
import com.auction.auth.exception.InvalidCredentialsException;
import com.auction.auth.exception.ResourceNotFoundException;
import com.auction.auth.exception.UserAlreadyExistsException;
import com.auction.auth.repository.UserRepository;
import com.auction.auth.security.JwtTokenProvider;
import com.auction.auth.service.impl.AuthServiceImpl;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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
    private LoginRequest loginRequestWithEmail;
    private LoginRequest loginRequestWithUsername;

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

        loginRequestWithEmail = LoginRequest.builder()
                .email("john@example.com")
                .password("password123")
                .build();

        loginRequestWithUsername = LoginRequest.builder()
                .username("john_buyer")
                .password("password123")
                .build();
    }

    @Test
    @DisplayName("Test successful registration hashes password and returns JWT")
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
        assertEquals("Bearer", response.getTokenType());
        assertEquals("john@example.com", response.getEmail());
        assertEquals("john_buyer", response.getUsername());
        assertEquals(Role.BUYER, response.getRole());
        verify(passwordEncoder, times(1)).encode("password123");
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Test registration with duplicate email throws UserAlreadyExistsException")
    void testRegisterDuplicateEmailThrowsException() {
        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        UserAlreadyExistsException exception = assertThrows(
                UserAlreadyExistsException.class,
                () -> authService.register(registerRequest)
        );
        assertTrue(exception.getMessage().contains("Email is already registered"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Test registration with duplicate username throws UserAlreadyExistsException")
    void testRegisterDuplicateUsernameThrowsException() {
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("john_buyer")).thenReturn(true);

        UserAlreadyExistsException exception = assertThrows(
                UserAlreadyExistsException.class,
                () -> authService.register(registerRequest)
        );
        assertTrue(exception.getMessage().contains("Username is already taken"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Test successful login with email returns JWT")
    void testLoginSuccessWithEmail() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("password123", "encoded_password")).thenReturn(true);
        when(jwtTokenProvider.generateToken(1L, "john@example.com", "john_buyer", Role.BUYER)).thenReturn("mock.jwt.token");
        when(jwtTokenProvider.getExpirationMs()).thenReturn(86400000L);

        AuthResponse response = authService.login(loginRequestWithEmail);

        assertNotNull(response);
        assertEquals("mock.jwt.token", response.getToken());
        assertEquals(1L, response.getUserId());
        assertEquals("john@example.com", response.getEmail());
        assertEquals("john_buyer", response.getUsername());
    }

    @Test
    @DisplayName("Test successful login with username returns JWT")
    void testLoginSuccessWithUsername() {
        when(userRepository.findByEmail("john_buyer")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("john_buyer")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("password123", "encoded_password")).thenReturn(true);
        when(jwtTokenProvider.generateToken(1L, "john@example.com", "john_buyer", Role.BUYER)).thenReturn("mock.jwt.token");
        when(jwtTokenProvider.getExpirationMs()).thenReturn(86400000L);

        AuthResponse response = authService.login(loginRequestWithUsername);

        assertNotNull(response);
        assertEquals("mock.jwt.token", response.getToken());
        assertEquals(1L, response.getUserId());
        assertEquals("john_buyer", response.getUsername());
    }

    @Test
    @DisplayName("Test login with wrong password throws InvalidCredentialsException")
    void testLoginWrongPasswordThrowsException() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("password123", "encoded_password")).thenReturn(false);

        InvalidCredentialsException exception = assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login(loginRequestWithEmail)
        );
        assertTrue(exception.getMessage().contains("Invalid username/email or password"));
    }

    @Test
    @DisplayName("Test login with unknown user throws InvalidCredentialsException")
    void testLoginUnknownUserThrowsException() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("unknown@example.com")).thenReturn(Optional.empty());

        LoginRequest unknownRequest = LoginRequest.builder()
                .email("unknown@example.com")
                .password("password123")
                .build();

        InvalidCredentialsException exception = assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login(unknownRequest)
        );
        assertTrue(exception.getMessage().contains("Invalid username/email or password"));
    }

    @Test
    @DisplayName("Test login with missing identifier throws InvalidCredentialsException")
    void testLoginMissingIdentifierThrowsException() {
        LoginRequest emptyRequest = new LoginRequest();
        emptyRequest.setPassword("password123");

        assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login(emptyRequest)
        );
    }

    @Test
    @DisplayName("Test validate token with valid token returns valid response")
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
        assertEquals(Role.BUYER, response.getRole());
    }

    @Test
    @DisplayName("Test validate token with invalid or malformed token returns invalid response")
    void testValidateTokenInvalid() {
        when(jwtTokenProvider.validateToken("invalid.token")).thenReturn(false);

        ValidateTokenResponse response = authService.validateToken("invalid.token");

        assertFalse(response.isValid());
        assertEquals("Token is invalid, expired, or missing", response.getMessage());
    }

    @Test
    @DisplayName("Test validate token with null or empty string returns invalid response")
    void testValidateTokenNull() {
        ValidateTokenResponse response = authService.validateToken(null);
        assertFalse(response.isValid());

        ValidateTokenResponse responseEmpty = authService.validateToken("");
        assertFalse(responseEmpty.isValid());
    }

    @Test
    @DisplayName("Test getUserById returns UserResponse when found")
    void testGetUserByIdSuccess() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

        var response = authService.getUserById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("john_buyer", response.getUsername());
    }

    @Test
    @DisplayName("Test getUserById throws ResourceNotFoundException when not found")
    void testGetUserByIdNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> authService.getUserById(999L));
    }
}
