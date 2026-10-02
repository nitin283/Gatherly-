package com.example.gatherly.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.gatherly.model.Event;
import com.example.gatherly.model.EventStatus;
import com.example.gatherly.model.User;
import com.example.gatherly.repository.BookingRepository;
import com.example.gatherly.repository.EventRepository;

/** Applies event ownership, approval, editing, and deletion rules. */
@Service
public class EventService {

    private final EventRepository eventRepository;
    private final BookingRepository bookingRepository;

    public EventService(EventRepository eventRepository, BookingRepository bookingRepository) {
        this.eventRepository = eventRepository;
        this.bookingRepository = bookingRepository;
    }

    public Event createEvent(Event event, User organizer) {
        event.setOrganizer(organizer);
        // New and edited events require an administrator's review before public listing.
        event.setStatus(EventStatus.PENDING);
        return eventRepository.save(event);
    }

    public Event getById(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id " + id));
    }

    public List<Event> getApprovedEvents() {
        return eventRepository.findByStatusOrderByEventDateAsc(EventStatus.APPROVED);
    }

    public List<Event> searchApprovedByTitle(String title) {
        return eventRepository.findByStatusAndTitleContainingIgnoreCaseOrderByEventDateAsc(
                EventStatus.APPROVED, title);
    }

    public List<Event> filterApprovedByCategory(String category) {
        return eventRepository.findByStatusAndCategoryIgnoreCaseOrderByEventDateAsc(
                EventStatus.APPROVED, category);
    }

    public List<Event> getEventsByOrganizer(Long organizerId) {
        return eventRepository.findByOrganizerIdOrderByCreatedAtDesc(organizerId);
    }

    public List<Event> getPendingEvents() {
        return eventRepository.findByStatusOrderByCreatedAtAsc(EventStatus.PENDING);
    }

    @Transactional
    public Event approveEvent(Long eventId) {
        Event event = getById(eventId);
        if (event.getStatus() != EventStatus.PENDING) {
            throw new BusinessRuleException("Only pending events can be approved.");
        }
        event.setStatus(EventStatus.APPROVED);
        return eventRepository.save(event);
    }

    @Transactional
    public Event rejectEvent(Long eventId, String reason) {
        Event event = getById(eventId);
        if (event.getStatus() != EventStatus.PENDING) {
            throw new BusinessRuleException("Only pending events can be rejected.");
        }
        event.setStatus(EventStatus.REJECTED);
        event.setRejectionReason(reason);
        return eventRepository.save(event);
    }

    @Transactional
    public Event updateEvent(Long eventId, Long requesterId, Event changes) {
        Event event = getById(eventId);
        if (!event.getOrganizer().getId().equals(requesterId)) {
            throw new BusinessRuleException("You can only edit your own events.");
        }
        event.setTitle(changes.getTitle());
        event.setDescription(changes.getDescription());
        event.setCategory(changes.getCategory());
        event.setVenue(changes.getVenue());
        event.setEventDate(changes.getEventDate());
        // Editing an approved event sends it through approval again.
        event.setStatus(EventStatus.PENDING);
        event.setRejectionReason(null);
        return eventRepository.save(event);
    }

    @Transactional
    public void deleteEvent(Long eventId, Long requesterId) {
        Event event = getById(eventId);
        if (!event.getOrganizer().getId().equals(requesterId)) {
            throw new BusinessRuleException("You can only delete your own events.");
        }
        if (bookingRepository.countByEventId(eventId) > 0) {
            // Keep existing booking records and their foreign-key references intact.
            throw new BusinessRuleException("This event has bookings and cannot be deleted.");
        }
        eventRepository.delete(event);
    }

    public long countByStatus(EventStatus status) {
        return eventRepository.countByStatus(status);
    }
}
