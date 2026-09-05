package com.auction.auth.controller;

import com.auction.auth.dto.AuthResponse;
import com.auction.auth.dto.LoginRequest;
import com.auction.auth.dto.RegisterRequest;
import com.auction.auth.dto.ValidateTokenResponse;
import com.auction.auth.entity.Role;
import com.auction.auth.exception.InvalidCredentialsException;
import com.auction.auth.exception.UserAlreadyExistsException;
import com.auction.auth.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @Test
    @DisplayName("Test POST /api/auth/register success")
    void testRegisterEndpointSuccess() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("testuser")
                .email("test@example.com")
                .password("secret123")
                .role(Role.BUYER)
                .build();

        AuthResponse authResponse = AuthResponse.builder()
                .token("jwt.token.mock")
                .tokenType("Bearer")
                .userId(1L)
                .username("testuser")
                .email("test@example.com")
                .role(Role.BUYER)
                .expiresInMs(86400000L)
                .build();

        when(authService.register(any(RegisterRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("jwt.token.mock"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    @DisplayName("Test registration with duplicate username returns 409 Conflict")
    void testRegisterDuplicateUsername() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("existinguser")
                .email("new@example.com")
                .password("secret123")
                .build();

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new UserAlreadyExistsException("Username is already taken: existinguser"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Username is already taken: existinguser"));
    }

    @Test
    @DisplayName("Test registration with duplicate email returns 409 Conflict")
    void testRegisterDuplicateEmail() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("newuser")
                .email("existing@example.com")
                .password("secret123")
                .build();

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new UserAlreadyExistsException("Email is already registered: existing@example.com"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Email is already registered: existing@example.com"));
    }

    @Test
    @DisplayName("Test registration with invalid email returns 400 Bad Request")
    void testRegisterInvalidEmail() throws Exception {
        RegisterRequest invalidRequest = RegisterRequest.builder()
                .username("validuser")
                .email("not-an-email")
                .password("secret123")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.email").exists());
    }

    @Test
    @DisplayName("Test registration with weak/invalid input returns 400 Bad Request")
    void testRegisterWeakInvalidInput() throws Exception {
        RegisterRequest invalidRequest = RegisterRequest.builder()
                .username("ab") // too short (min 3)
                .email("") // blank
                .password("123") // too short (min 6)
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.username").exists())
                .andExpect(jsonPath("$.validationErrors.email").exists())
                .andExpect(jsonPath("$.validationErrors.password").exists());
    }

    @Test
    @DisplayName("Test POST /api/auth/login success with email")
    void testLoginEndpointSuccessWithEmail() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("test@example.com")
                .password("secret123")
                .build();

        AuthResponse authResponse = AuthResponse.builder()
                .token("jwt.token.mock")
                .tokenType("Bearer")
                .userId(1L)
                .username("testuser")
                .email("test@example.com")
                .role(Role.BUYER)
                .expiresInMs(86400000L)
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt.token.mock"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.userId").value(1L));
    }

    @Test
    @DisplayName("Test POST /api/auth/login success with username")
    void testLoginEndpointSuccessWithUsername() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .username("testuser")
                .password("secret123")
                .build();

        AuthResponse authResponse = AuthResponse.builder()
                .token("jwt.token.mock")
                .tokenType("Bearer")
                .userId(1L)
                .username("testuser")
                .email("test@example.com")
                .role(Role.BUYER)
                .expiresInMs(86400000L)
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt.token.mock"))
                .andExpect(jsonPath("$.username").value("testuser"));
    }

    @Test
    @DisplayName("Test login with wrong password returns 401 Unauthorized")
    void testLoginWrongPassword() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("test@example.com")
                .password("wrongpassword")
                .build();

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new InvalidCredentialsException("Invalid username/email or password"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Invalid username/email or password"));
    }

    @Test
    @DisplayName("Test login with unknown user returns 401 Unauthorized")
    void testLoginUnknownUser() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("nonexistent@example.com")
                .password("secret123")
                .build();

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new InvalidCredentialsException("Invalid username/email or password"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("Test GET /api/auth/validate with valid token returns 200 OK")
    void testValidateTokenSuccess() throws Exception {
        ValidateTokenResponse validateResponse = ValidateTokenResponse.builder()
                .valid(true)
                .userId(1L)
                .email("test@example.com")
                .role(Role.BUYER)
                .message("Token is valid")
                .build();

        when(authService.validateToken("valid.jwt.token")).thenReturn(validateResponse);

        mockMvc.perform(get("/api/auth/validate")
                        .header(HttpHeaders.AUTHORIZATION, "valid.jwt.token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.userId").value(1L))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    @DisplayName("Test GET /api/auth/validate with invalid or malformed token returns 401 Unauthorized")
    void testValidateTokenInvalid() throws Exception {
        ValidateTokenResponse validateResponse = ValidateTokenResponse.builder()
                .valid(false)
                .message("Token is invalid, expired, or missing")
                .build();

        when(authService.validateToken("malformed.jwt")).thenReturn(validateResponse);

        mockMvc.perform(get("/api/auth/validate")
                        .header(HttpHeaders.AUTHORIZATION, "malformed.jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.message").value("Token is invalid, expired, or missing"));
    }

    @Test
    @DisplayName("Test GET /api/auth/validate with missing token returns 401 Unauthorized")
    void testValidateTokenMissing() throws Exception {
        ValidateTokenResponse validateResponse = ValidateTokenResponse.builder()
                .valid(false)
                .message("Token is invalid, expired, or missing")
                .build();

        when(authService.validateToken(null)).thenReturn(validateResponse);

        mockMvc.perform(get("/api/auth/validate"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.valid").value(false));
    }
}
