package com.kampus.etkinlik.ticket;

import com.kampus.etkinlik.event.Event;
import com.kampus.etkinlik.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "ticket")
public class Ticket {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "event_id")
  private Event event;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id")
  private User user;

  /** QR koduna gömülen, tahmin edilemez bilet kodu. */
  @Column(nullable = false, unique = true)
  private String code;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TicketStatus status = TicketStatus.ACTIVE;

  @Column(name = "issued_at", nullable = false, updatable = false)
  private Instant issuedAt;

  @Column(name = "checked_in_at")
  private Instant checkedInAt;

  protected Ticket() {}

  public Ticket(Event event, User user, String code, Instant issuedAt) {
    this.event = event;
    this.user = user;
    this.code = code;
    this.issuedAt = issuedAt;
  }

  public void checkIn(Instant at) {
    this.status = TicketStatus.USED;
    this.checkedInAt = at;
  }

  public void cancel() {
    this.status = TicketStatus.CANCELLED;
  }

  public Long getId() {
    return id;
  }

  public Event getEvent() {
    return event;
  }

  public User getUser() {
    return user;
  }

  public String getCode() {
    return code;
  }

  public TicketStatus getStatus() {
    return status;
  }

  public Instant getIssuedAt() {
    return issuedAt;
  }

  public Instant getCheckedInAt() {
    return checkedInAt;
  }
}
