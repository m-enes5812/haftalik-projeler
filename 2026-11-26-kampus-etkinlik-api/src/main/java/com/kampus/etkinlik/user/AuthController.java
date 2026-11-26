package com.kampus.etkinlik.user;

import com.kampus.etkinlik.security.CurrentUser;
import com.kampus.etkinlik.user.UserDtos.LoginRequest;
import com.kampus.etkinlik.user.UserDtos.RegisterRequest;
import com.kampus.etkinlik.user.UserDtos.RoleChangeRequest;
import com.kampus.etkinlik.user.UserDtos.TokenResponse;
import com.kampus.etkinlik.user.UserDtos.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Kimlik ve kullanıcılar")
public class AuthController {

  private final UserService service;
  private final CurrentUser currentUser;

  public AuthController(UserService service, CurrentUser currentUser) {
    this.service = service;
    this.currentUser = currentUser;
  }

  @PostMapping("/api/auth/register")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Öğrenci olarak kayıt ol")
  public UserResponse register(@Valid @RequestBody RegisterRequest req) {
    return UserResponse.of(service.register(req));
  }

  @PostMapping("/api/auth/login")
  @Operation(summary = "Giriş yap, JWT al")
  public TokenResponse login(@Valid @RequestBody LoginRequest req) {
    return service.login(req);
  }

  @GetMapping("/api/me")
  @Operation(summary = "Oturumdaki kullanıcı")
  public UserResponse me() {
    return UserResponse.of(service.get(currentUser.id()));
  }

  @PatchMapping("/api/admin/users/{id}/role")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Kullanıcının rolünü değiştir (admin)")
  public UserResponse changeRole(@PathVariable long id, @Valid @RequestBody RoleChangeRequest req) {
    return UserResponse.of(service.changeRole(id, req.role()));
  }
}
