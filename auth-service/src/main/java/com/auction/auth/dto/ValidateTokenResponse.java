package com.auction.auth.dto;

import com.auction.auth.entity.Role;

public class ValidateTokenResponse {

    private boolean valid;
    private Long userId;
    private String email;
    private Role role;
    private String message;

    public ValidateTokenResponse() {
    }

    public ValidateTokenResponse(boolean valid, Long userId, String email, Role role, String message) {
        this.valid = valid;
        this.userId = userId;
        this.email = email;
        this.role = role;
        this.message = message;
    }

    public static ValidateTokenResponseBuilder builder() {
        return new ValidateTokenResponseBuilder();
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public static class ValidateTokenResponseBuilder {
        private boolean valid;
        private Long userId;
        private String email;
        private Role role;
        private String message;

        ValidateTokenResponseBuilder() {
        }

        public ValidateTokenResponseBuilder valid(boolean valid) {
            this.valid = valid;
            return this;
        }

        public ValidateTokenResponseBuilder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public ValidateTokenResponseBuilder email(String email) {
            this.email = email;
            return this;
        }

        public ValidateTokenResponseBuilder role(Role role) {
            this.role = role;
            return this;
        }

        public ValidateTokenResponseBuilder message(String message) {
            this.message = message;
            return this;
        }

        public ValidateTokenResponse build() {
            return new ValidateTokenResponse(valid, userId, email, role, message);
        }
    }
}
