package com.kampus.etkinlik.user;

import com.kampus.etkinlik.common.ApiException;
import com.kampus.etkinlik.security.JwtService;
import com.kampus.etkinlik.user.UserDtos.LoginRequest;
import com.kampus.etkinlik.user.UserDtos.RegisterRequest;
import com.kampus.etkinlik.user.UserDtos.TokenResponse;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

  private final UserRepository users;
  private final PasswordEncoder encoder;
  private final JwtService jwt;

  public UserService(UserRepository users, PasswordEncoder encoder, JwtService jwt) {
    this.users = users;
    this.encoder = encoder;
    this.jwt = jwt;
  }

  /** Herkese açık kayıt her zaman STUDENT rolüyle yapılır; kulüp yetkisini admin verir. */
  @Transactional
  public User register(RegisterRequest req) {
    String email = normalize(req.email());
    if (users.existsByEmailIgnoreCase(email)) {
      throw ApiException.conflict("Bu e-posta zaten kayıtlı");
    }
    return users.save(
        new User(email, encoder.encode(req.password()), req.fullName().trim(), Role.STUDENT));
  }

  @Transactional(readOnly = true)
  public TokenResponse login(LoginRequest req) {
    User user =
        users
            .findByEmailIgnoreCase(normalize(req.email()))
            .filter(u -> encoder.matches(req.password(), u.getPasswordHash()))
            // Kullanıcı var mı yok mu sızdırmamak için tek tip mesaj.
            .orElseThrow(
                () -> new ApiException(HttpStatus.UNAUTHORIZED, "E-posta veya şifre hatalı"));
    return jwt.issue(user);
  }

  @Transactional
  public User changeRole(long userId, Role role) {
    User user = users.findById(userId).orElseThrow(() -> ApiException.notFound("Kullanıcı"));
    user.setRole(role);
    return user;
  }

  @Transactional(readOnly = true)
  public Page<User> list(Pageable pageable) {
    return users.findAll(pageable);
  }

  @Transactional(readOnly = true)
  public User get(long userId) {
    return users.findById(userId).orElseThrow(() -> ApiException.notFound("Kullanıcı"));
  }

  static String normalize(String email) {
    return email.trim().toLowerCase(Locale.ROOT);
  }
}
