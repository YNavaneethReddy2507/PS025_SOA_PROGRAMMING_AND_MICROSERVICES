package com.auction.auth.service;

import com.auction.auth.dto.*;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    ValidateTokenResponse validateToken(String token);
    UserResponse getUserById(Long userId);
}
