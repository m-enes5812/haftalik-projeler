package com.kampus.etkinlik.event;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventRepository extends JpaRepository<Event, Long> {

  Page<Event> findByStatusAndStartsAtAfter(EventStatus status, Instant after, Pageable pageable);

  /**
   * Kontenjan kontrolü için etkinlik satırını kilitler (SELECT ... FOR UPDATE). Aynı etkinliğe
   * eşzamanlı kayıtlar sıraya girer; böylece kontenjan asla aşılmaz.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from Event e where e.id = :id")
  Optional<Event> findByIdForUpdate(@Param("id") Long id);
}
