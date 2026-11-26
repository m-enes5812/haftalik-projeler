package com.kampus.etkinlik.ticket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kampus.etkinlik.support.IntegrationTestBase;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class TicketFlowIT extends IntegrationTestBase {

  @Test
  void kayit_qr_giris_ve_istatistik_uctan_uca() throws Exception {
    String club = newClub();
    String student = newStudent();
    long eventId = publishedEvent(club, 10);

    var ticket =
        body(
            call(post("/api/events/{id}/tickets", eventId), student, null)
                .andExpect(status().isCreated()));
    long ticketId = ticket.get("id").asLong();
    String code = ticket.get("code").asText();

    call(get("/api/tickets/{id}/qr", ticketId), student, null)
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.IMAGE_PNG));

    call(get("/api/events/{id}", eventId), null, null)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.remainingSeats").value(9));

    call(post("/api/events/{id}/check-in", eventId), club, Map.of("code", code))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.ticketId").value(ticketId));
    call(post("/api/events/{id}/check-in", eventId), club, Map.of("code", code))
        .andExpect(status().isConflict());

    call(get("/api/events/{id}/stats", eventId), club, null)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.registered").value(1))
        .andExpect(jsonPath("$.checkedIn").value(1));
  }

  @Test
  void iptal_edilen_bilet_yeri_bosaltir() throws Exception {
    String club = newClub();
    String first = newStudent();
    String second = newStudent();
    long eventId = publishedEvent(club, 1);

    long ticketId =
        body(call(post("/api/events/{id}/tickets", eventId), first, null)
                .andExpect(status().isCreated()))
            .get("id")
            .asLong();
    call(post("/api/events/{id}/tickets", eventId), second, null).andExpect(status().isConflict());

    call(delete("/api/tickets/{id}", ticketId), first, null)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CANCELLED"));
    call(post("/api/events/{id}/tickets", eventId), second, null).andExpect(status().isCreated());
    // İptal eden öğrenci yeniden kayıt olabilir mi? Yer kalmadığı için hayır.
    call(post("/api/events/{id}/tickets", eventId), first, null).andExpect(status().isConflict());
  }

  @Test
  void etkinlik_iptali_aktif_biletleri_iptal_eder() throws Exception {
    String club = newClub();
    String student = newStudent();
    long eventId = publishedEvent(club, 5);
    call(post("/api/events/{id}/tickets", eventId), student, null).andExpect(status().isCreated());

    call(post("/api/events/{id}/cancel", eventId), club, null).andExpect(status().isOk());

    call(get("/api/me/tickets"), student, null)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].status").value("CANCELLED"));
  }

  @Test
  void esZamanli_kayitlarda_kontenjan_asilmaz() throws Exception {
    String club = newClub();
    long eventId = publishedEvent(club, 5);
    int students = 20;
    List<String> tokens = new ArrayList<>();
    for (int i = 0; i < students; i++) {
      tokens.add(newStudent());
    }

    CountDownLatch start = new CountDownLatch(1);
    List<Future<Integer>> results = new ArrayList<>();
    try (var pool = Executors.newFixedThreadPool(students)) {
      for (String token : tokens) {
        Callable<Integer> task =
            () -> {
              start.await();
              return call(post("/api/events/{id}/tickets", eventId), token, null)
                  .andReturn()
                  .getResponse()
                  .getStatus();
            };
        results.add(pool.submit(task));
      }
      start.countDown();
      int created = 0;
      int conflict = 0;
      for (Future<Integer> f : results) {
        int s = f.get();
        if (s == 201) {
          created++;
        } else if (s == 409) {
          conflict++;
        }
      }
      assertThat(created).isEqualTo(5);
      assertThat(conflict).isEqualTo(15);
    }

    call(get("/api/events/{id}", eventId), null, null)
        .andExpect(jsonPath("$.remainingSeats").value(0));
  }
}
