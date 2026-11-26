package com.kampus.etkinlik.security;

import com.kampus.etkinlik.user.User;
import com.kampus.etkinlik.user.UserDtos.TokenResponse;
import java.time.Clock;
import java.time.Instant;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/** Kısa ömürlü HS256 erişim belirteci üretir. Doğrulama Spring resource-server tarafında. */
@Service
public class JwtService {

  public static final String ROLE_CLAIM = "role";

  private final JwtEncoder encoder;
  private final JwtProperties props;
  private final Clock clock;

  public JwtService(JwtEncoder encoder, JwtProperties props, Clock clock) {
    this.encoder = encoder;
    this.props = props;
    this.clock = clock;
  }

  public TokenResponse issue(User user) {
    Instant now = clock.instant();
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .issuer(props.issuer())
            .subject(String.valueOf(user.getId()))
            .issuedAt(now)
            .expiresAt(now.plus(props.ttl()))
            .claim(ROLE_CLAIM, user.getRole().name())
            .claim("email", user.getEmail())
            .build();
    JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
    String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    return new TokenResponse(token, "Bearer", props.ttl().toSeconds());
  }
}
