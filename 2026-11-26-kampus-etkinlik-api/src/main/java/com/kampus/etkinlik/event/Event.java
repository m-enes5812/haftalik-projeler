package com.kampus.etkinlik.event;

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
@Table(name = "event")
public class Event {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String title;

  private String description;

  @Column(nullable = false)
  private String location;

  @Column(name = "starts_at", nullable = false)
  private Instant startsAt;

  @Column(name = "ends_at", nullable = false)
  private Instant endsAt;

  @Column(nullable = false)
  private int capacity;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private EventStatus status = EventStatus.DRAFT;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "organizer_id")
  private User organizer;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  protected Event() {}

  public Event(
      String title,
      String description,
      String location,
      Instant startsAt,
      Instant endsAt,
      int capacity,
      User organizer) {
    this.organizer = organizer;
    update(title, description, location, startsAt, endsAt, capacity);
  }

  public final void update(
      String title,
      String description,
      String location,
      Instant startsAt,
      Instant endsAt,
      int capacity) {
    this.title = title;
    this.description = description;
    this.location = location;
    this.startsAt = startsAt;
    this.endsAt = endsAt;
    this.capacity = capacity;
  }

  public boolean isOpenForRegistration(Instant now) {
    return status == EventStatus.PUBLISHED && now.isBefore(startsAt);
  }

  public Long getId() {
    return id;
  }

  public String getTitle() {
    return title;
  }

  public String getDescription() {
    return description;
  }

  public String getLocation() {
    return location;
  }

  public Instant getStartsAt() {
    return startsAt;
  }

  public Instant getEndsAt() {
    return endsAt;
  }

  public int getCapacity() {
    return capacity;
  }

  public EventStatus getStatus() {
    return status;
  }

  public void setStatus(EventStatus status) {
    this.status = status;
  }

  public User getOrganizer() {
    return organizer;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
