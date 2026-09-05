package com.auction.auth.dto;

import jakarta.validation.constraints.NotBlank;

public class LoginRequest {

    private String email;
    private String username;
    private String usernameOrEmail;

    @NotBlank(message = "Password cannot be blank")
    private String password;

    public LoginRequest() {
    }

    public LoginRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public LoginRequest(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }

    public LoginRequest(String username, String email, String usernameOrEmail, String password) {
        this.username = username;
        this.email = email;
        this.usernameOrEmail = usernameOrEmail;
        this.password = password;
    }

    public static LoginRequestBuilder builder() {
        return new LoginRequestBuilder();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUsernameOrEmail() {
        return usernameOrEmail;
    }

    public void setUsernameOrEmail(String usernameOrEmail) {
        this.usernameOrEmail = usernameOrEmail;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getIdentifier() {
        if (usernameOrEmail != null && !usernameOrEmail.trim().isEmpty()) {
            return usernameOrEmail.trim();
        }
        if (email != null && !email.trim().isEmpty()) {
            return email.trim();
        }
        if (username != null && !username.trim().isEmpty()) {
            return username.trim();
        }
        return null;
    }

    public static class LoginRequestBuilder {
        private String email;
        private String username;
        private String usernameOrEmail;
        private String password;

        LoginRequestBuilder() {
        }

        public LoginRequestBuilder email(String email) {
            this.email = email;
            return this;
        }

        public LoginRequestBuilder username(String username) {
            this.username = username;
            return this;
        }

        public LoginRequestBuilder usernameOrEmail(String usernameOrEmail) {
            this.usernameOrEmail = usernameOrEmail;
            return this;
        }

        public LoginRequestBuilder password(String password) {
            this.password = password;
            return this;
        }

        public LoginRequest build() {
            return new LoginRequest(username, email, usernameOrEmail, password);
        }
    }
}
