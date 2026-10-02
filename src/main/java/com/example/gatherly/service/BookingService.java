package com.example.gatherly.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.gatherly.dto.TicketSelectionRequest;
import com.example.gatherly.model.Booking;
import com.example.gatherly.model.BookingItem;
import com.example.gatherly.model.BookingStatus;
import com.example.gatherly.model.EventStatus;
import com.example.gatherly.model.TicketType;
import com.example.gatherly.model.User;
import com.example.gatherly.repository.BookingRepository;
import com.example.gatherly.repository.TicketTypeRepository;

/** Applies booking, stock, pricing, and cancellation rules transactionally. */
@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final TicketTypeRepository ticketTypeRepository;

    public BookingService(BookingRepository bookingRepository,
                           TicketTypeRepository ticketTypeRepository) {
        this.bookingRepository = bookingRepository;
        this.ticketTypeRepository = ticketTypeRepository;
    }

    // ticketTypeId -> quantity requested, e.g. {5: 2} means "2 of ticket type 5"
    @Transactional
    public Booking bookTickets(User attendee, Long ticketTypeId, int quantity) {
        if (quantity < 1) {
            throw new BusinessRuleException("Quantity must be at least 1.");
        }

        TicketType ticketType = ticketTypeRepository.findWithLockById(ticketTypeId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket type not found."));

        if (ticketType.getEvent().getStatus() != EventStatus.APPROVED) {
            throw new BusinessRuleException("This event is not open for booking.");
        }

        if (ticketType.getAvailableQuantity() < quantity) {
            throw new BusinessRuleException(
                    "Only " + ticketType.getAvailableQuantity() + " ticket(s) left for this type.");
        }

        // Reduce stock
        ticketType.setAvailableQuantity(ticketType.getAvailableQuantity() - quantity);
        ticketTypeRepository.save(ticketType);

        // Build the booking
        BigDecimal totalAmount = ticketType.getPrice().multiply(BigDecimal.valueOf(quantity));

        Booking booking = new Booking();
        booking.setAttendee(attendee);
        booking.setEvent(ticketType.getEvent());
        booking.setBookingReference(generateReference());
        booking.setBookingDate(LocalDateTime.now());
        booking.setTotalAmount(totalAmount);
        booking.setStatus(BookingStatus.CONFIRMED);

        BookingItem item = new BookingItem();
        item.setTicketType(ticketType);
        item.setQuantity(quantity);
        item.setUnitPrice(ticketType.getPrice());
        booking.addItem(item);

        return bookingRepository.save(booking);
    }

    // Books several ticket types from the SAME event in one transaction.
    // selections: list of (ticketTypeId, quantity) pairs, all validated together.
    // Any rule failure rolls back stock updates and prevents a partial booking.
    @Transactional
    public Booking bookMultipleTickets(User attendee, Long eventId, List<TicketSelectionRequest> selections) {

        // Keep only rows the attendee actually filled in (quantity > 0)
        List<TicketSelectionRequest> chosen = selections.stream()
                .filter(s -> s.getQuantity() > 0)
                .toList();

        if (chosen.isEmpty()) {
            throw new BusinessRuleException("Select at least one ticket to book.");
        }

        Booking booking = new Booking();
        booking.setAttendee(attendee);
        booking.setBookingReference(generateReference());
        booking.setBookingDate(LocalDateTime.now());
        booking.setStatus(BookingStatus.CONFIRMED);
        BigDecimal total = BigDecimal.ZERO;

        for (TicketSelectionRequest selection : chosen) {
            TicketType ticketType = ticketTypeRepository.findWithLockById(selection.getTicketTypeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Ticket type not found."));

            if (!ticketType.getEvent().getId().equals(eventId)) {
                throw new BusinessRuleException("Ticket type does not belong to this event.");
            }
            if (ticketType.getEvent().getStatus() != EventStatus.APPROVED) {
                throw new BusinessRuleException("This event is not open for booking.");
            }
            if (ticketType.getAvailableQuantity() < selection.getQuantity()) {
                throw new BusinessRuleException(
                        "Only " + ticketType.getAvailableQuantity() + " " + ticketType.getTypeName() + " ticket(s) left.");
            }

            ticketType.setAvailableQuantity(ticketType.getAvailableQuantity() - selection.getQuantity());
            ticketTypeRepository.save(ticketType);

            if (booking.getEvent() == null) {
                booking.setEvent(ticketType.getEvent());
            }

            BookingItem item = new BookingItem();
            item.setTicketType(ticketType);
            item.setQuantity(selection.getQuantity());
            item.setUnitPrice(ticketType.getPrice());
            booking.addItem(item);

            total = total.add(ticketType.getPrice().multiply(BigDecimal.valueOf(selection.getQuantity())));
        }

        booking.setTotalAmount(total);
        return bookingRepository.save(booking);
    }

    @Transactional
    public Booking cancelBooking(Long bookingId, Long requesterId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found."));

        if (!booking.getAttendee().getId().equals(requesterId)) {
            throw new BusinessRuleException("You can only cancel your own bookings.");
        }
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BusinessRuleException("This booking is already cancelled.");
        }
        if (booking.getEvent().getEventDate().isBefore(LocalDateTime.now())) {
            throw new BusinessRuleException("Cannot cancel a booking for a past event.");
        }

        // Return every item to stock in the same transaction as the cancellation.
        for (BookingItem item : booking.getItems()) {
            TicketType tt = ticketTypeRepository.findWithLockById(item.getTicketType().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Ticket type not found."));
            tt.setAvailableQuantity(tt.getAvailableQuantity() + item.getQuantity());
            ticketTypeRepository.save(tt);
        }

        booking.setStatus(BookingStatus.CANCELLED);
        return bookingRepository.save(booking);
    }

    public List<Booking> getBookingsForAttendee(Long attendeeId) {
        return bookingRepository.findByAttendeeIdOrderByBookingDateDesc(attendeeId);
    }

    public List<Booking> getBookingsForEvent(Long eventId) {
        return bookingRepository.findByEventId(eventId);
    }

    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    public List<Booking> getAllBookingsForAdmin() {
        return bookingRepository.findAllByOrderByBookingDateDesc();
    }

    private String generateReference() {
        return "GTH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
