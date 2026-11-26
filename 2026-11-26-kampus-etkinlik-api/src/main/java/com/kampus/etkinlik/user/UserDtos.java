package com.kampus.etkinlik.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class UserDtos {

  private UserDtos() {}

  public record RegisterRequest(
      @NotBlank @Email @Size(max = 254) String email,
      @NotBlank @Size(min = 8, max = 72, message = "Şifre 8-72 karakter olmalı") String password,
      @NotBlank @Size(max = 120) String fullName) {}

  public record LoginRequest(@NotBlank String email, @NotBlank String password) {}

  public record TokenResponse(String accessToken, String tokenType, long expiresInSeconds) {}

  public record RoleChangeRequest(@NotNull Role role) {}

  public record UserResponse(Long id, String email, String fullName, Role role, Instant createdAt) {
    public static UserResponse of(User u) {
      return new UserResponse(
          u.getId(), u.getEmail(), u.getFullName(), u.getRole(), u.getCreatedAt());
    }
  }
}
