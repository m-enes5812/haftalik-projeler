package com.kampus.etkinlik.event;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class EventDtos {

  private EventDtos() {}

  public record EventRequest(
      @NotBlank @Size(max = 160) String title,
      @Size(max = 4000) String description,
      @NotBlank @Size(max = 200) String location,
      @NotNull @Future Instant startsAt,
      @NotNull Instant endsAt,
      @Min(1) @Max(100_000) int capacity) {

    @AssertTrue(message = "Bitiş zamanı başlangıçtan sonra olmalı")
    public boolean isEndsAfterStart() {
      return startsAt == null || endsAt == null || endsAt.isAfter(startsAt);
    }
  }

  public record EventResponse(
      Long id,
      String title,
      String description,
      String location,
      Instant startsAt,
      Instant endsAt,
      int capacity,
      long remainingSeats,
      EventStatus status,
      Long organizerId,
      String organizerName) {

    public static EventResponse of(Event e, long activeTickets) {
      return new EventResponse(
          e.getId(),
          e.getTitle(),
          e.getDescription(),
          e.getLocation(),
          e.getStartsAt(),
          e.getEndsAt(),
          e.getCapacity(),
          Math.max(0, e.getCapacity() - activeTickets),
          e.getStatus(),
          e.getOrganizer().getId(),
          e.getOrganizer().getFullName());
    }
  }

  public record EventStats(
      Long eventId, int capacity, long registered, long checkedIn, long cancelled) {}
}
