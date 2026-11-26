package com.kampus.etkinlik.ticket;

import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.stereotype.Component;

/**
 * 256 bit rastgele, URL-güvenli bilet kodu üretir. Kod sıralı ID içermediği için tahmin edilemez;
 * sahte QR üretmek pratikte imkânsızdır.
 */
@Component
public class TicketCodeGenerator {

  private final SecureRandom random = new SecureRandom();

  public String next() {
    byte[] bytes = new byte[32];
    random.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }
}
