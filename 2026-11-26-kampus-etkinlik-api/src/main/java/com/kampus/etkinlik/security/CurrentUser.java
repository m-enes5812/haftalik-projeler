package com.kampus.etkinlik.security;

import com.kampus.etkinlik.user.Role;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/** İstekteki JWT'den kullanıcı kimliğini ve rolünü okur. */
@Component
public class CurrentUser {

  public long id() {
    return Long.parseLong(jwt().getSubject());
  }

  public Role role() {
    return Role.valueOf(jwt().getClaimAsString(JwtService.ROLE_CLAIM));
  }

  public boolean isAdmin() {
    return role() == Role.ADMIN;
  }

  private Jwt jwt() {
    return (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
  }
}
