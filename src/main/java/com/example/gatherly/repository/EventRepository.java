package com.example.gatherly.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.gatherly.model.Event;
import com.example.gatherly.model.EventStatus;

public interface EventRepository extends JpaRepository<Event, Long> {

    // Attendee browsing: approved events, soonest first
    List<Event> findByStatusOrderByEventDateAsc(EventStatus status);

    // Attendee search by title
    List<Event> findByStatusAndTitleContainingIgnoreCaseOrderByEventDateAsc(
            EventStatus status, String title);

    // Attendee filter by category
    List<Event> findByStatusAndCategoryIgnoreCaseOrderByEventDateAsc(
            EventStatus status, String category);

    // Organizer dashboard: my events
    List<Event> findByOrganizerIdOrderByCreatedAtDesc(Long organizerId);

    // Admin: events waiting for approval, and statistics
    // The admin dashboard displays each pending event's organizer name.
    @EntityGraph(attributePaths = "organizer")
    List<Event> findByStatusOrderByCreatedAtAsc(EventStatus status);

    long countByStatus(EventStatus status);
}
