package com.example.gatherly.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.gatherly.model.TicketType;

/** Database queries for ticket types and their event-specific names. */
public interface TicketTypeRepository extends JpaRepository<TicketType, Long> {
    boolean existsByEventIdAndTypeName(Long eventId, com.example.gatherly.model.TicketTypeName typeName);

    List<TicketType> findByEventId(Long eventId);
    // Lock the selected row so concurrent bookings cannot both sell the same remaining ticket.
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    java.util.Optional<TicketType> findWithLockById(Long id);
}
