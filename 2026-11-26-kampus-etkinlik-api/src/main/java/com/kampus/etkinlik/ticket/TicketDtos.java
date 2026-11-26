package com.kampus.etkinlik.ticket;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

public final class TicketDtos {

  private TicketDtos() {}

  public record TicketResponse(
      Long id,
      Long eventId,
      String eventTitle,
      Instant eventStartsAt,
      String code,
      TicketStatus status,
      Instant issuedAt,
      Instant checkedInAt) {

    public static TicketResponse of(Ticket t) {
      return new TicketResponse(
          t.getId(),
          t.getEvent().getId(),
          t.getEvent().getTitle(),
          t.getEvent().getStartsAt(),
          t.getCode(),
          t.getStatus(),
          t.getIssuedAt(),
          t.getCheckedInAt());
    }
  }

  public record CheckInRequest(@NotBlank String code) {}

  public record CheckInResponse(Long ticketId, String attendeeName, Instant checkedInAt) {}
}
