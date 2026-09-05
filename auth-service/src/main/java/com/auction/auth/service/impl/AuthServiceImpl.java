package com.auction.auth.service.impl;

import com.auction.auth.dto.*;
import com.auction.auth.entity.Role;
import com.auction.auth.entity.User;
import com.auction.auth.exception.InvalidCredentialsException;
import com.auction.auth.exception.ResourceNotFoundException;
import com.auction.auth.exception.UserAlreadyExistsException;
import com.auction.auth.repository.UserRepository;
import com.auction.auth.security.JwtTokenProvider;
import com.auction.auth.service.AuthService;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Processing user registration for username: {}", request.getUsername());

        String email = request.getEmail().trim().toLowerCase();
        String username = request.getUsername().trim();

        if (userRepository.existsByEmail(email)) {
            log.warn("Registration failed: Email is already registered");
            throw new UserAlreadyExistsException("Email is already registered: " + email);
        }

        if (userRepository.existsByUsername(username)) {
            log.warn("Registration failed: Username is already taken");
            throw new UserAlreadyExistsException("Username is already taken: " + username);
        }

        User user = User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole() != null ? request.getRole() : Role.BUYER)
                .build();

        User savedUser = userRepository.save(user);
        log.info("User registered successfully with ID: {}", savedUser.getId());

        String token = jwtTokenProvider.generateToken(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getUsername(),
                savedUser.getRole()
        );

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(savedUser.getId())
                .username(savedUser.getUsername())
                .email(savedUser.getEmail())
                .role(savedUser.getRole())
                .expiresInMs(jwtTokenProvider.getExpirationMs())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String identifier = request.getIdentifier();
        if (identifier == null || identifier.trim().isEmpty()) {
            log.warn("Login failed: Identifier is missing");
            throw new InvalidCredentialsException("Username or email must be provided");
        }

        String cleanIdentifier = identifier.trim();
        log.info("Processing user login attempt");

        User user = userRepository.findByEmail(cleanIdentifier.toLowerCase())
                .or(() -> userRepository.findByUsername(cleanIdentifier))
                .orElseThrow(() -> {
                    log.warn("Login failed: User not found");
                    return new InvalidCredentialsException("Invalid username/email or password");
                });

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Login failed: Password mismatch for user ID: {}", user.getId());
            throw new InvalidCredentialsException("Invalid username/email or password");
        }

        String token = jwtTokenProvider.generateToken(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getRole()
        );

        log.info("User logged in successfully: ID {}", user.getId());

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .expiresInMs(jwtTokenProvider.getExpirationMs())
                .build();
    }

    @Override
    public ValidateTokenResponse validateToken(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        if (token == null || !jwtTokenProvider.validateToken(token)) {
            return ValidateTokenResponse.builder()
                    .valid(false)
                    .message("Token is invalid, expired, or missing")
                    .build();
        }

        Claims claims = jwtTokenProvider.extractAllClaims(token);
        Long userId = claims.get("userId", Long.class);
        String email = claims.getSubject();
        String roleStr = claims.get("role", String.class);

        return ValidateTokenResponse.builder()
                .valid(true)
                .userId(userId)
                .email(email)
                .role(roleStr != null ? Role.valueOf(roleStr) : Role.BUYER)
                .message("Token is valid")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
