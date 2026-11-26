package com.kampus.etkinlik.event;

import com.kampus.etkinlik.event.EventDtos.EventResponse;
import com.kampus.etkinlik.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Etkinlikler")
public class MyEventsController {

  private final EventService service;
  private final CurrentUser me;

  public MyEventsController(EventService service, CurrentUser me) {
    this.service = service;
    this.me = me;
  }

  @GetMapping("/api/me/events")
  @PreAuthorize("hasAnyRole('CLUB','ADMIN')")
  @Operation(summary = "Düzenlediğim etkinlikler (taslaklar dahil)")
  public List<EventResponse> mine() {
    return service.organizedBy(me.id());
  }
}
