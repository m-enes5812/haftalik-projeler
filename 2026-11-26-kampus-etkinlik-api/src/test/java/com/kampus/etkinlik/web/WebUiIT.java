package com.kampus.etkinlik.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kampus.etkinlik.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;

/** Web arayüzünün dosyaları ve arayüzün kullandığı ek uçlar. */
class WebUiIT extends IntegrationTestBase {

  @Test
  void arayuz_dosyalari_tokensiz_acilir() throws Exception {
    call(get("/"), null, null).andExpect(status().isOk()).andExpect(forwardedUrl("index.html"));
    call(get("/index.html"), null, null)
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("<script src=\"/app.js\">")));
    call(get("/app.js"), null, null).andExpect(status().isOk());
    call(get("/style.css"), null, null).andExpect(status().isOk());
    call(get("/favicon.svg"), null, null).andExpect(status().isOk());
  }

  @Test
  void kulup_kendi_etkinliklerini_taslaklar_dahil_gorur() throws Exception {
    String club = newClub();
    String other = newClub();
    long published = publishedEvent(club, 5);
    long draft = body(call(post("/api/events"), club, eventBody(3))).get("id").asLong();
    long foreign = publishedEvent(other, 5);

    call(get("/api/me/events"), club, null)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[*].id", hasItem((int) published)))
        .andExpect(jsonPath("$[*].id", hasItem((int) draft)))
        .andExpect(jsonPath("$[*].id", not(hasItem((int) foreign))))
        .andExpect(jsonPath("$[?(@.id == " + published + ")].remainingSeats").value(5));
  }

  @Test
  void ogrenci_kendi_etkinlik_listesine_ve_kullanici_listesine_erisemez() throws Exception {
    String student = newStudent();
    call(get("/api/me/events"), student, null).andExpect(status().isForbidden());
    call(get("/api/admin/users"), student, null).andExpect(status().isForbidden());
    call(get("/api/admin/users"), null, null).andExpect(status().isUnauthorized());
  }

  @Test
  void admin_kullanicilari_listeler_ve_sifre_ozeti_donmez() throws Exception {
    String admin = login(ADMIN_EMAIL, ADMIN_PASSWORD);
    call(get("/api/admin/users"), admin, null)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[*].email", hasItem(ADMIN_EMAIL)))
        .andExpect(jsonPath("$.content[0].passwordHash").doesNotExist());
  }
}
