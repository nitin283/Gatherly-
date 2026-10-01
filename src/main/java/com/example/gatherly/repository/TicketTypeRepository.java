package com.example.gatherly.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.gatherly.model.TicketType;

public interface TicketTypeRepository extends JpaRepository<TicketType, Long> {
    boolean existsByEventIdAndTypeName(Long eventId, com.example.gatherly.model.TicketTypeName typeName);

    List<TicketType> findByEventId(Long eventId);
        // Prevents two simultaneous bookings from both reading "5 left" and overselling
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    java.util.Optional<TicketType> findWithLockById(Long id);
}
