package com.kampus.etkinlik.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, Duration ttl, String issuer) {

  public JwtProperties {
    if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
      throw new IllegalStateException("app.jwt.secret en az 32 bayt olmalı (HS256)");
    }
    if (ttl == null) {
      ttl = Duration.ofHours(2);
    }
  }
}
