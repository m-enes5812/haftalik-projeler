package com.kampus.etkinlik.ticket;

import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

  long countByEventIdAndStatusIn(Long eventId, Collection<TicketStatus> statuses);

  boolean existsByEventIdAndUserIdAndStatusIn(
      Long eventId, Long userId, Collection<TicketStatus> statuses);

  /** Aynı bileti eşzamanlı iki görevlinin okutmasına karşı satırı kilitler. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select t from Ticket t join fetch t.user where t.code = :code")
  Optional<Ticket> findByCodeForUpdate(@Param("code") String code);

  @EntityGraph(attributePaths = "event")
  List<Ticket> findByUserIdOrderByIssuedAtDesc(Long userId);

  interface EventCount {
    Long getEventId();

    Long getTotal();
  }

  @Query(
      """
      select t.event.id as eventId, count(t) as total from Ticket t
      where t.event.id in :ids and t.status in :statuses
      group by t.event.id
      """)
  List<EventCount> countByEventIds(
      @Param("ids") Collection<Long> ids, @Param("statuses") Collection<TicketStatus> statuses);

  @Modifying
  @Query(
      "update Ticket t set t.status = com.kampus.etkinlik.ticket.TicketStatus.CANCELLED"
          + " where t.event.id = :eventId and t.status ="
          + " com.kampus.etkinlik.ticket.TicketStatus.ACTIVE")
  int cancelAllActive(@Param("eventId") Long eventId);
}
