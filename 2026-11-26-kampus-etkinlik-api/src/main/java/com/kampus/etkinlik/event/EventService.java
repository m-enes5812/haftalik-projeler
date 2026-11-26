package com.kampus.etkinlik.event;

import com.kampus.etkinlik.common.ApiException;
import com.kampus.etkinlik.event.EventDtos.EventRequest;
import com.kampus.etkinlik.event.EventDtos.EventResponse;
import com.kampus.etkinlik.event.EventDtos.EventStats;
import com.kampus.etkinlik.ticket.TicketRepository;
import com.kampus.etkinlik.ticket.TicketStatus;
import com.kampus.etkinlik.user.User;
import com.kampus.etkinlik.user.UserRepository;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventService {

  static final List<TicketStatus> LIVE = List.of(TicketStatus.ACTIVE, TicketStatus.USED);

  private final EventRepository events;
  private final TicketRepository tickets;
  private final UserRepository users;
  private final Clock clock;

  public EventService(
      EventRepository events, TicketRepository tickets, UserRepository users, Clock clock) {
    this.events = events;
    this.tickets = tickets;
    this.users = users;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public Page<EventResponse> upcoming(Pageable pageable) {
    Page<Event> page =
        events.findByStatusAndStartsAtAfter(EventStatus.PUBLISHED, clock.instant(), pageable);
    Map<Long, Long> counts = liveCounts(page.map(Event::getId).getContent());
    return page.map(e -> EventResponse.of(e, counts.getOrDefault(e.getId(), 0L)));
  }

  /** Düzenleyicinin tüm etkinlikleri (taslak ve iptal dahil), en yenisi önce. */
  @Transactional(readOnly = true)
  public List<EventResponse> organizedBy(long organizerId) {
    List<Event> list = events.findByOrganizerIdOrderByStartsAtDesc(organizerId);
    Map<Long, Long> counts = liveCounts(list.stream().map(Event::getId).toList());
    return list.stream().map(e -> EventResponse.of(e, counts.getOrDefault(e.getId(), 0L))).toList();
  }

  /** Verilen etkinliklerin dolulukları tek sorguda (N+1 yok). */
  private Map<Long, Long> liveCounts(List<Long> ids) {
    return ids.isEmpty()
        ? Map.of()
        : tickets.countByEventIds(ids, LIVE).stream()
            .collect(
                Collectors.toMap(
                    TicketRepository.EventCount::getEventId,
                    TicketRepository.EventCount::getTotal));
  }

  /** Herkese açık görünüm: taslaklar sadece sahibine/admine görünür. */
  @Transactional(readOnly = true)
  public EventResponse getPublic(long id) {
    Event e = find(id);
    if (e.getStatus() == EventStatus.DRAFT) {
      throw ApiException.notFound("Etkinlik");
    }
    return toResponse(e);
  }

  @Transactional
  public EventResponse create(EventRequest req, long organizerId) {
    User organizer = users.getReferenceById(organizerId);
    Event e =
        events.save(
            new Event(
                req.title().trim(),
                req.description(),
                req.location().trim(),
                req.startsAt(),
                req.endsAt(),
                req.capacity(),
                organizer));
    return EventResponse.of(e, 0);
  }

  @Transactional
  public EventResponse update(long id, EventRequest req, long actorId, boolean admin) {
    Event e = findOwned(id, actorId, admin);
    if (e.getStatus() == EventStatus.CANCELLED) {
      throw ApiException.conflict("İptal edilmiş etkinlik düzenlenemez");
    }
    long live = tickets.countByEventIdAndStatusIn(id, LIVE);
    if (req.capacity() < live) {
      throw ApiException.conflict(
          "Kontenjan mevcut kayıt sayısının (" + live + ") altına düşürülemez");
    }
    e.update(
        req.title().trim(),
        req.description(),
        req.location().trim(),
        req.startsAt(),
        req.endsAt(),
        req.capacity());
    return EventResponse.of(e, live);
  }

  @Transactional
  public EventResponse publish(long id, long actorId, boolean admin) {
    Event e = findOwned(id, actorId, admin);
    if (e.getStatus() != EventStatus.DRAFT) {
      throw ApiException.conflict("Sadece taslak etkinlik yayınlanabilir");
    }
    if (!e.getStartsAt().isAfter(clock.instant())) {
      throw ApiException.conflict("Başlangıcı geçmiş etkinlik yayınlanamaz");
    }
    e.setStatus(EventStatus.PUBLISHED);
    return toResponse(e);
  }

  @Transactional
  public EventResponse cancel(long id, long actorId, boolean admin) {
    Event e = findOwned(id, actorId, admin);
    if (e.getStatus() == EventStatus.CANCELLED) {
      throw ApiException.conflict("Etkinlik zaten iptal edilmiş");
    }
    e.setStatus(EventStatus.CANCELLED);
    tickets.cancelAllActive(id);
    return toResponse(e);
  }

  @Transactional(readOnly = true)
  public EventStats stats(long id, long actorId, boolean admin) {
    Event e = findOwned(id, actorId, admin);
    return new EventStats(
        id,
        e.getCapacity(),
        tickets.countByEventIdAndStatusIn(id, LIVE),
        tickets.countByEventIdAndStatusIn(id, List.of(TicketStatus.USED)),
        tickets.countByEventIdAndStatusIn(id, List.of(TicketStatus.CANCELLED)));
  }

  /** Etkinliği getirir ve aktörün sahibi (ya da admin) olduğunu doğrular. */
  public Event findOwned(long id, long actorId, boolean admin) {
    Event e = find(id);
    if (!admin && !e.getOrganizer().getId().equals(actorId)) {
      throw ApiException.forbidden("Bu etkinlik üzerinde yetkiniz yok");
    }
    return e;
  }

  private Event find(long id) {
    return events.findById(id).orElseThrow(() -> ApiException.notFound("Etkinlik"));
  }

  private EventResponse toResponse(Event e) {
    return EventResponse.of(e, tickets.countByEventIdAndStatusIn(e.getId(), LIVE));
  }
}
