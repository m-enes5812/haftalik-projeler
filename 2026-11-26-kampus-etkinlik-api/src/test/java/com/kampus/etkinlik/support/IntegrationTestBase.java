package com.kampus.etkinlik.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Tüm entegrasyon testleri gerçek bir PostgreSQL konteynerine karşı çalışır. Konteyner sınıflar
 * arasında paylaşılır, Spring bağlamı da önbellekte tutulduğu için test süresi kısa kalır.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class IntegrationTestBase {

  @ServiceConnection
  protected static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:16-alpine");

  static {
    POSTGRES.start();
  }

  protected static final String ADMIN_EMAIL = "admin@kampus.test";
  protected static final String ADMIN_PASSWORD = "Admin-Sifre-123";

  @Autowired protected MockMvc mvc;
  @Autowired protected ObjectMapper json;

  protected ResultActions call(MockHttpServletRequestBuilder req, String token, Object body)
      throws Exception {
    if (token != null) {
      req.header("Authorization", "Bearer " + token);
    }
    if (body != null) {
      req.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(body));
    }
    return mvc.perform(req);
  }

  protected JsonNode body(ResultActions result) throws Exception {
    return json.readTree(result.andReturn().getResponse().getContentAsByteArray());
  }

  /** Yeni bir öğrenci kaydeder ve token'ını döner. */
  protected String newStudent() throws Exception {
    String email = "ogr-" + UUID.randomUUID() + "@kampus.test";
    register(email);
    return login(email, "Guclu-Sifre-1");
  }

  /** Yeni bir kullanıcı kaydeder, admin olarak CLUB rolü verir ve yeni token'ını döner. */
  protected String newClub() throws Exception {
    String email = "kulup-" + UUID.randomUUID() + "@kampus.test";
    long id = register(email);
    call(
            patch("/api/admin/users/{id}/role", id),
            login(ADMIN_EMAIL, ADMIN_PASSWORD),
            Map.of("role", "CLUB"))
        .andExpect(status().isOk());
    return login(email, "Guclu-Sifre-1");
  }

  protected long register(String email) throws Exception {
    var res =
        call(
                post("/api/auth/register"),
                null,
                Map.of("email", email, "password", "Guclu-Sifre-1", "fullName", "Test Kişi"))
            .andExpect(status().isCreated());
    return body(res).get("id").asLong();
  }

  protected String login(String email, String password) throws Exception {
    var res =
        call(post("/api/auth/login"), null, Map.of("email", email, "password", password))
            .andExpect(status().isOk());
    return body(res).get("accessToken").asText();
  }

  /** Geçerli bir etkinlik oluşturma isteği gövdesi. */
  protected Map<String, Object> eventBody(int capacity) {
    Instant start = Instant.now().plus(7, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
    return Map.of(
        "title",
        "Test Etkinliği",
        "location",
        "Amfi 1",
        "startsAt",
        start.toString(),
        "endsAt",
        start.plus(2, ChronoUnit.HOURS).toString(),
        "capacity",
        capacity);
  }

  /** Kulüp adına yayınlanmış bir etkinlik oluşturur ve ID'sini döner. */
  protected long publishedEvent(String clubToken, int capacity) throws Exception {
    var created =
        call(post("/api/events"), clubToken, eventBody(capacity)).andExpect(status().isCreated());
    long id = body(created).get("id").asLong();
    call(post("/api/events/{id}/publish", id), clubToken, null).andExpect(status().isOk());
    return id;
  }
}
