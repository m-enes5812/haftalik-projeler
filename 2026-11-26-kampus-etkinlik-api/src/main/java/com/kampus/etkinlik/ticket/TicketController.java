package com.kampus.etkinlik.ticket;

import com.kampus.etkinlik.security.CurrentUser;
import com.kampus.etkinlik.ticket.TicketDtos.CheckInRequest;
import com.kampus.etkinlik.ticket.TicketDtos.CheckInResponse;
import com.kampus.etkinlik.ticket.TicketDtos.TicketResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Biletler")
public class TicketController {

  private final TicketService service;
  private final CurrentUser me;

  public TicketController(TicketService service, CurrentUser me) {
    this.service = service;
    this.me = me;
  }

  @PostMapping("/api/events/{eventId}/tickets")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasRole('STUDENT')")
  @Operation(summary = "Etkinliğe kayıt ol ve bilet al")
  public TicketResponse register(@PathVariable long eventId) {
    return service.register(eventId, me.id());
  }

  @GetMapping("/api/me/tickets")
  @Operation(summary = "Biletlerim")
  public List<TicketResponse> mine() {
    return service.myTickets(me.id());
  }

  @DeleteMapping("/api/tickets/{id}")
  @Operation(summary = "Biletimi iptal et")
  public TicketResponse cancel(@PathVariable long id) {
    return service.cancel(id, me.id());
  }

  @GetMapping(value = "/api/tickets/{id}/qr", produces = MediaType.IMAGE_PNG_VALUE)
  @Operation(summary = "Biletin QR kodu (PNG)")
  public ResponseEntity<byte[]> qr(@PathVariable long id) {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .contentType(MediaType.IMAGE_PNG)
        .body(service.qrPng(id, me.id()));
  }

  @PostMapping("/api/events/{eventId}/check-in")
  @PreAuthorize("hasAnyRole('CLUB','ADMIN')")
  @Operation(summary = "Kapıda QR kodu ile giriş")
  public CheckInResponse checkIn(
      @PathVariable long eventId, @Valid @RequestBody CheckInRequest req) {
    return service.checkIn(eventId, req.code(), me.id(), me.isAdmin());
  }
}
