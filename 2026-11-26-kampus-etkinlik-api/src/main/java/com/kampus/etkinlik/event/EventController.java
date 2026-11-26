package com.kampus.etkinlik.event;

import com.kampus.etkinlik.event.EventDtos.EventRequest;
import com.kampus.etkinlik.event.EventDtos.EventResponse;
import com.kampus.etkinlik.event.EventDtos.EventStats;
import com.kampus.etkinlik.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events")
@Tag(name = "Etkinlikler")
public class EventController {

  private final EventService service;
  private final CurrentUser me;

  public EventController(EventService service, CurrentUser me) {
    this.service = service;
    this.me = me;
  }

  @GetMapping
  @Operation(summary = "Yaklaşan yayınlanmış etkinlikler (sayfalı)")
  public Page<EventResponse> upcoming(
      @PageableDefault(size = 20, sort = "startsAt", direction = Sort.Direction.ASC)
          Pageable pageable) {
    return service.upcoming(pageable);
  }

  @GetMapping("/{id}")
  @Operation(summary = "Etkinlik detayı")
  public EventResponse get(@PathVariable long id) {
    return service.getPublic(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAnyRole('CLUB','ADMIN')")
  @Operation(summary = "Taslak etkinlik oluştur (kulüp/admin)")
  public EventResponse create(@Valid @RequestBody EventRequest req) {
    return service.create(req, me.id());
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('CLUB','ADMIN')")
  @Operation(summary = "Etkinliği güncelle (sahibi/admin)")
  public EventResponse update(@PathVariable long id, @Valid @RequestBody EventRequest req) {
    return service.update(id, req, me.id(), me.isAdmin());
  }

  @PostMapping("/{id}/publish")
  @PreAuthorize("hasAnyRole('CLUB','ADMIN')")
  @Operation(summary = "Etkinliği yayınla")
  public EventResponse publish(@PathVariable long id) {
    return service.publish(id, me.id(), me.isAdmin());
  }

  @PostMapping("/{id}/cancel")
  @PreAuthorize("hasAnyRole('CLUB','ADMIN')")
  @Operation(summary = "Etkinliği iptal et (tüm aktif biletler de iptal olur)")
  public EventResponse cancel(@PathVariable long id) {
    return service.cancel(id, me.id(), me.isAdmin());
  }

  @GetMapping("/{id}/stats")
  @PreAuthorize("hasAnyRole('CLUB','ADMIN')")
  @Operation(summary = "Kayıt / giriş istatistikleri")
  public EventStats stats(@PathVariable long id) {
    return service.stats(id, me.id(), me.isAdmin());
  }
}
