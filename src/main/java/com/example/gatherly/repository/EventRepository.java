package com.example.gatherly.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.gatherly.model.Event;
import com.example.gatherly.model.EventStatus;

/** Database queries for events, including filtered public and admin lists. */
public interface EventRepository extends JpaRepository<Event, Long> {

    // Event ownership is checked by organizer edit, delete, and manage routes.
    @Override
    @EntityGraph(attributePaths = "organizer")
    Optional<Event> findById(Long id);

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
