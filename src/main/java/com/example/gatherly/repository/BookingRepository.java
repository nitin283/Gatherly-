package com.example.gatherly.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.gatherly.model.Booking;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    // Attendee: booking history, newest first
    // The dashboard template displays the event, booking items, and ticket names.
    // Fetch those relationships while the repository query's persistence context is open.
    @EntityGraph(attributePaths = {"event", "items", "items.ticketType"})
    List<Booking> findByAttendeeIdOrderByBookingDateDesc(Long attendeeId);

    // Organizer: bookings of one event
    List<Booking> findByEventId(Long eventId);

    Optional<Booking> findByBookingReference(String bookingReference);

    boolean existsByBookingReference(String bookingReference);
}
