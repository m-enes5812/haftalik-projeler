package com.kampus.etkinlik.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kampus.etkinlik.support.IntegrationTestBase;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SecurityIT extends IntegrationTestBase {

  @Test
  void tokensiz_korumali_uc_401() throws Exception {
    call(get("/api/me"), null, null).andExpect(status().isUnauthorized());
    call(get("/api/me/tickets"), null, null).andExpect(status().isUnauthorized());
  }

  @Test
  void bozuk_token_401() throws Exception {
    call(get("/api/me"), "bu.bir.token.degil", null).andExpect(status().isUnauthorized());
  }

  @Test
  void herkese_acik_uclar_tokensiz_calisir() throws Exception {
    call(get("/api/events"), null, null).andExpect(status().isOk());
    call(get("/actuator/health"), null, null).andExpect(status().isOk());
  }

  @Test
  void ogrenci_etkinlik_olusturamaz_ve_rol_degistiremez() throws Exception {
    String student = newStudent();
    call(post("/api/events"), student, eventBody(10)).andExpect(status().isForbidden());
    call(patch("/api/admin/users/{id}/role", 1), student, Map.of("role", "ADMIN"))
        .andExpect(status().isForbidden());
  }

  @Test
  void kulup_bilet_alamaz_ve_baskasinin_etkinligine_giris_yapamaz() throws Exception {
    String owner = newClub();
    String otherClub = newClub();
    long eventId = publishedEvent(owner, 3);

    call(post("/api/events/{id}/tickets", eventId), otherClub, null)
        .andExpect(status().isForbidden());
    call(post("/api/events/{id}/check-in", eventId), otherClub, Map.of("code", "x"))
        .andExpect(status().isForbidden());
    call(get("/api/events/{id}/stats", eventId), otherClub, null).andExpect(status().isForbidden());
  }

  @Test
  void kayit_her_zaman_ogrenci_rolu_verir_ve_ayni_eposta_409() throws Exception {
    String email = "tekrar@kampus.test";
    register(email);
    call(
            post("/api/auth/register"),
            null,
            Map.of("email", "TEKRAR@kampus.test", "password", "Guclu-Sifre-1", "fullName", "X"))
        .andExpect(status().isConflict());
    call(get("/api/me"), login(email, "Guclu-Sifre-1"), null)
        .andExpect(jsonPath("$.role").value("STUDENT"));
  }

  @Test
  void yanlis_sifre_401_ve_ayni_mesaj() throws Exception {
    call(post("/api/auth/login"), null, Map.of("email", ADMIN_EMAIL, "password", "yanlis"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.detail").value("E-posta veya şifre hatalı"));
    call(post("/api/auth/login"), null, Map.of("email", "yok@kampus.test", "password", "yanlis"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.detail").value("E-posta veya şifre hatalı"));
  }

  @Test
  void gecersiz_istek_400_ve_alan_hatalari() throws Exception {
    call(post("/api/auth/register"), null, Map.of("email", "bozuk", "password", "1"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.email").exists())
        .andExpect(jsonPath("$.errors.password").exists());
  }
}
