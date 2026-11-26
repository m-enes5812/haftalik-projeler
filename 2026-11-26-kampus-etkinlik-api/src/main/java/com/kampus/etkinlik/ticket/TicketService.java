package com.kampus.etkinlik.ticket;

import com.kampus.etkinlik.common.ApiException;
import com.kampus.etkinlik.event.Event;
import com.kampus.etkinlik.event.EventRepository;
import com.kampus.etkinlik.event.EventService;
import com.kampus.etkinlik.event.EventStatus;
import com.kampus.etkinlik.ticket.TicketDtos.CheckInResponse;
import com.kampus.etkinlik.ticket.TicketDtos.TicketResponse;
import com.kampus.etkinlik.user.User;
import com.kampus.etkinlik.user.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketService {

  static final List<TicketStatus> LIVE = List.of(TicketStatus.ACTIVE, TicketStatus.USED);

  private final TicketRepository tickets;
  private final EventRepository events;
  private final EventService eventService;
  private final UserRepository users;
  private final TicketCodeGenerator codes;
  private final QrCodeRenderer qr;
  private final Clock clock;

  public TicketService(
      TicketRepository tickets,
      EventRepository events,
      EventService eventService,
      UserRepository users,
      TicketCodeGenerator codes,
      QrCodeRenderer qr,
      Clock clock) {
    this.tickets = tickets;
    this.events = events;
    this.eventService = eventService;
    this.users = users;
    this.codes = codes;
    this.qr = qr;
    this.clock = clock;
  }

  /**
   * Öğrenciye bilet keser. Etkinlik satırı {@code SELECT ... FOR UPDATE} ile kilitlendiği için aynı
   * etkinliğe gelen eşzamanlı istekler sıraya girer ve kapasite asla aşılmaz.
   */
  @Transactional
  public TicketResponse register(long eventId, long userId) {
    Event event =
        events.findByIdForUpdate(eventId).orElseThrow(() -> ApiException.notFound("Etkinlik"));
    Instant now = clock.instant();
    if (event.getStatus() == EventStatus.DRAFT) {
      throw ApiException.notFound("Etkinlik");
    }
    if (!event.isOpenForRegistration(now)) {
      throw ApiException.conflict("Etkinlik kayda açık değil");
    }
    if (tickets.existsByEventIdAndUserIdAndStatusIn(eventId, userId, LIVE)) {
      throw ApiException.conflict("Bu etkinlik için zaten biletiniz var");
    }
    if (tickets.countByEventIdAndStatusIn(eventId, LIVE) >= event.getCapacity()) {
      throw ApiException.conflict("Etkinlik kontenjanı dolu");
    }
    User user = users.getReferenceById(userId);
    Ticket ticket = tickets.save(new Ticket(event, user, codes.next(), now));
    return TicketResponse.of(ticket);
  }

  @Transactional(readOnly = true)
  public List<TicketResponse> myTickets(long userId) {
    return tickets.findByUserIdOrderByIssuedAtDesc(userId).stream()
        .map(TicketResponse::of)
        .toList();
  }

  @Transactional
  public TicketResponse cancel(long ticketId, long userId) {
    Ticket t = findOwnTicket(ticketId, userId);
    if (t.getStatus() != TicketStatus.ACTIVE) {
      throw ApiException.conflict("Sadece aktif bilet iptal edilebilir");
    }
    t.cancel();
    return TicketResponse.of(t);
  }

  @Transactional(readOnly = true)
  public byte[] qrPng(long ticketId, long userId) {
    Ticket t = findOwnTicket(ticketId, userId);
    if (t.getStatus() == TicketStatus.CANCELLED) {
      throw ApiException.conflict("İptal edilmiş biletin QR kodu yok");
    }
    return qr.png(t.getCode());
  }

  /** Kapıdaki görevli QR'dan okunan kodu gönderir; bilet tek kullanımlıktır. */
  @Transactional
  public CheckInResponse checkIn(long eventId, String code, long actorId, boolean admin) {
    Event event = eventService.findOwned(eventId, actorId, admin);
    if (event.getStatus() != EventStatus.PUBLISHED) {
      throw ApiException.conflict("Etkinlik girişe açık değil");
    }
    Ticket t =
        tickets
            .findByCodeForUpdate(code)
            .filter(x -> x.getEvent().getId().equals(eventId))
            .orElseThrow(() -> ApiException.notFound("Bu etkinliğe ait bilet"));
    switch (t.getStatus()) {
      case USED -> throw ApiException.conflict("Bilet daha önce kullanılmış");
      case CANCELLED -> throw ApiException.conflict("Bilet iptal edilmiş");
      case ACTIVE -> t.checkIn(clock.instant());
    }
    return new CheckInResponse(t.getId(), t.getUser().getFullName(), t.getCheckedInAt());
  }

  private Ticket findOwnTicket(long ticketId, long userId) {
    Ticket t = tickets.findById(ticketId).orElseThrow(() -> ApiException.notFound("Bilet"));
    // Başkasının biletinin varlığını sızdırmamak için 403 yerine 404 döner.
    if (!t.getUser().getId().equals(userId)) {
      throw ApiException.notFound("Bilet");
    }
    return t;
  }
}
