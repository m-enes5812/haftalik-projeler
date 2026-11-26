package com.kampus.etkinlik.ticket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kampus.etkinlik.common.ApiException;
import com.kampus.etkinlik.event.Event;
import com.kampus.etkinlik.event.EventRepository;
import com.kampus.etkinlik.event.EventService;
import com.kampus.etkinlik.event.EventStatus;
import com.kampus.etkinlik.user.Role;
import com.kampus.etkinlik.user.User;
import com.kampus.etkinlik.user.UserRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

  private static final Instant NOW = Instant.parse("2026-12-01T10:00:00Z");

  @Mock TicketRepository tickets;
  @Mock EventRepository events;
  @Mock EventService eventService;
  @Mock UserRepository users;
  @Mock TicketCodeGenerator codes;
  @Mock QrCodeRenderer qr;

  TicketService service;
  User organizer;
  User student;
  Event event;

  @BeforeEach
  void setUp() {
    service =
        new TicketService(
            tickets, events, eventService, users, codes, qr, Clock.fixed(NOW, ZoneOffset.UTC));
    organizer = user(1L, "Kulüp", Role.CLUB);
    student = user(2L, "Ayşe Yılmaz", Role.STUDENT);
    event =
        new Event(
            "Hackathon",
            null,
            "Konferans Salonu",
            NOW.plus(Duration.ofDays(3)),
            NOW.plus(Duration.ofDays(3)).plus(Duration.ofHours(6)),
            2,
            organizer);
    ReflectionTestUtils.setField(event, "id", 10L);
    event.setStatus(EventStatus.PUBLISHED);
  }

  @Test
  void kayit_basarili_olursa_aktif_bilet_kesilir() {
    when(events.findByIdForUpdate(10L)).thenReturn(Optional.of(event));
    when(tickets.countByEventIdAndStatusIn(eq(10L), any())).thenReturn(1L);
    when(users.getReferenceById(2L)).thenReturn(student);
    when(codes.next()).thenReturn("kod-123");
    when(tickets.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

    var res = service.register(10L, 2L);

    assertThat(res.code()).isEqualTo("kod-123");
    assertThat(res.status()).isEqualTo(TicketStatus.ACTIVE);
    assertThat(res.issuedAt()).isEqualTo(NOW);
  }

  @Test
  void kontenjan_doluysa_409() {
    when(events.findByIdForUpdate(10L)).thenReturn(Optional.of(event));
    when(tickets.countByEventIdAndStatusIn(eq(10L), any())).thenReturn(2L);

    assertStatus(() -> service.register(10L, 2L), HttpStatus.CONFLICT);
    verify(tickets, never()).save(any());
  }

  @Test
  void ayni_ogrenci_ikinci_bilet_alamaz() {
    when(events.findByIdForUpdate(10L)).thenReturn(Optional.of(event));
    when(tickets.existsByEventIdAndUserIdAndStatusIn(eq(10L), eq(2L), any())).thenReturn(true);

    assertStatus(() -> service.register(10L, 2L), HttpStatus.CONFLICT);
  }

  @Test
  void taslak_etkinlik_disaridan_gorunmez() {
    event.setStatus(EventStatus.DRAFT);
    when(events.findByIdForUpdate(10L)).thenReturn(Optional.of(event));

    assertStatus(() -> service.register(10L, 2L), HttpStatus.NOT_FOUND);
  }

  @Test
  void baslamis_etkinlige_kayit_olunamaz() {
    ReflectionTestUtils.setField(event, "startsAt", NOW.minusSeconds(1));
    when(events.findByIdForUpdate(10L)).thenReturn(Optional.of(event));

    assertStatus(() -> service.register(10L, 2L), HttpStatus.CONFLICT);
  }

  @Test
  void giris_bileti_kullanildi_yapar_ve_ikinci_okutma_409() {
    Ticket t = ticket(TicketStatus.ACTIVE);
    when(eventService.findOwned(10L, 1L, false)).thenReturn(event);
    when(tickets.findByCodeForUpdate("kod")).thenReturn(Optional.of(t));

    var res = service.checkIn(10L, "kod", 1L, false);

    assertThat(res.attendeeName()).isEqualTo("Ayşe Yılmaz");
    assertThat(t.getStatus()).isEqualTo(TicketStatus.USED);
    assertThat(t.getCheckedInAt()).isEqualTo(NOW);
    assertStatus(() -> service.checkIn(10L, "kod", 1L, false), HttpStatus.CONFLICT);
  }

  @Test
  void baska_etkinligin_bileti_ile_giris_yapilamaz() {
    Event other =
        new Event("Başka", null, "B", NOW.plusSeconds(60), NOW.plusSeconds(120), 5, organizer);
    ReflectionTestUtils.setField(other, "id", 99L);
    Ticket t = new Ticket(other, student, "kod", NOW);
    when(eventService.findOwned(10L, 1L, false)).thenReturn(event);
    when(tickets.findByCodeForUpdate("kod")).thenReturn(Optional.of(t));

    assertStatus(() -> service.checkIn(10L, "kod", 1L, false), HttpStatus.NOT_FOUND);
    assertThat(t.getStatus()).isEqualTo(TicketStatus.ACTIVE);
  }

  @Test
  void iptal_edilmis_bilet_ile_giris_yapilamaz() {
    Ticket t = ticket(TicketStatus.CANCELLED);
    when(eventService.findOwned(10L, 1L, false)).thenReturn(event);
    when(tickets.findByCodeForUpdate("kod")).thenReturn(Optional.of(t));

    assertStatus(() -> service.checkIn(10L, "kod", 1L, false), HttpStatus.CONFLICT);
  }

  @Test
  void baskasinin_biletini_iptal_etmek_404() {
    Ticket t = ticket(TicketStatus.ACTIVE);
    ReflectionTestUtils.setField(t, "id", 5L);
    when(tickets.findById(5L)).thenReturn(Optional.of(t));

    assertStatus(() -> service.cancel(5L, 77L), HttpStatus.NOT_FOUND);
    assertThat(t.getStatus()).isEqualTo(TicketStatus.ACTIVE);
  }

  @Test
  void iptal_edilmis_biletin_qr_kodu_uretilmez() {
    Ticket t = ticket(TicketStatus.CANCELLED);
    when(tickets.findById(anyLong())).thenReturn(Optional.of(t));

    assertStatus(() -> service.qrPng(5L, 2L), HttpStatus.CONFLICT);
    verify(qr, never()).png(any());
  }

  private Ticket ticket(TicketStatus status) {
    Ticket t = new Ticket(event, student, "kod", NOW);
    if (status == TicketStatus.CANCELLED) {
      t.cancel();
    } else if (status == TicketStatus.USED) {
      t.checkIn(NOW);
    }
    return t;
  }

  private static User user(long id, String name, Role role) {
    User u = new User(name.toLowerCase() + "@kampus.test", "hash", name, role);
    ReflectionTestUtils.setField(u, "id", id);
    return u;
  }

  private static void assertStatus(Runnable call, HttpStatus expected) {
    assertThatThrownBy(call::run)
        .isInstanceOf(ApiException.class)
        .extracting(e -> ((ApiException) e).status())
        .isEqualTo(expected);
  }
}
