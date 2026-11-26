package com.kampus.etkinlik.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * İlk admin hesabını ADMIN_EMAIL / ADMIN_PASSWORD ortam değişkenlerinden oluşturur. Depoya gömülü
 * varsayılan şifre yoktur; sistemde zaten bir admin varsa hiçbir şey yapmaz.
 */
@Component
public class AdminBootstrap implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

  private final UserRepository users;
  private final PasswordEncoder encoder;
  private final String email;
  private final String password;

  public AdminBootstrap(
      UserRepository users,
      PasswordEncoder encoder,
      @Value("${app.bootstrap-admin.email:}") String email,
      @Value("${app.bootstrap-admin.password:}") String password) {
    this.users = users;
    this.encoder = encoder;
    this.email = email;
    this.password = password;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if (email.isBlank() || password.isBlank() || users.existsByRole(Role.ADMIN)) {
      return;
    }
    users.save(
        new User(UserService.normalize(email), encoder.encode(password), "Yönetici", Role.ADMIN));
    log.info("İlk admin hesabı oluşturuldu: {}", email);
  }
}
