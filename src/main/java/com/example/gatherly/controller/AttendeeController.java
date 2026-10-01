package com.example.gatherly.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.gatherly.dto.BookingRequest;
import com.example.gatherly.model.User;
import com.example.gatherly.service.BookingService;
import com.example.gatherly.service.BusinessRuleException;

@Controller
public class AttendeeController {

    private final BookingService bookingService;

    public AttendeeController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping("/dashboard/attendee")
    public String myBookings(@AuthenticationPrincipal User attendee, Model model) {
        List<com.example.gatherly.model.Booking> bookings =
                bookingService.getBookingsForAttendee(attendee.getId());
        model.addAttribute("bookings", bookings);
        return "dashboard/attendee";
    }

    @PostMapping("/events/{id}/book")
    public String book(@AuthenticationPrincipal User attendee,
                        @PathVariable Long id,
                        @ModelAttribute BookingRequest bookingRequest,
                        RedirectAttributes redirectAttributes) {
        try {
            var booking = bookingService.bookMultipleTickets(attendee, id, bookingRequest.getSelections());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Booking confirmed! Reference: " + booking.getBookingReference());
        } catch (BusinessRuleException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/events/" + id;
        }
        return "redirect:/dashboard/attendee";
    }

    @PostMapping("/dashboard/attendee/bookings/{id}/cancel")
    public String cancel(@AuthenticationPrincipal User attendee,
                          @PathVariable Long id,
                          RedirectAttributes redirectAttributes) {
        try {
            bookingService.cancelBooking(id, attendee.getId());
            redirectAttributes.addFlashAttribute("successMessage", "Booking cancelled.");
        } catch (BusinessRuleException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/dashboard/attendee";
    }
}