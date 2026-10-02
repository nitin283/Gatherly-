package com.example.gatherly.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.gatherly.model.EventStatus;
import com.example.gatherly.model.Role;
import com.example.gatherly.service.BookingService;
import com.example.gatherly.service.BusinessRuleException;
import com.example.gatherly.service.EventService;
import com.example.gatherly.service.UserService;

/** Provides administrator dashboard, event review, user, and booking routes. */
@Controller
public class AdminController {

    private final EventService eventService;
    private final UserService userService;
    private final BookingService bookingService;

    public AdminController(EventService eventService, UserService userService, BookingService bookingService) {
        this.eventService = eventService;
        this.userService = userService;
        this.bookingService = bookingService;
    }

    // ===== Dashboard: stats + pending events =====
    @GetMapping("/dashboard/admin")
    public String dashboard(Model model) {
        model.addAttribute("totalUsers", userService.getAllUsers().size());
        model.addAttribute("totalEvents",
                eventService.countByStatus(EventStatus.APPROVED)
                        + eventService.countByStatus(EventStatus.PENDING)
                        + eventService.countByStatus(EventStatus.REJECTED));
        model.addAttribute("pendingCount", eventService.countByStatus(EventStatus.PENDING));
        model.addAttribute("totalBookings", bookingService.getAllBookings().size());
        model.addAttribute("pendingEvents", eventService.getPendingEvents());
        return "dashboard/admin";
    }

    // ===== Approve =====
    @PostMapping("/dashboard/admin/events/{id}/approve")
    public String approve(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            eventService.approveEvent(id);
            redirectAttributes.addFlashAttribute("successMessage", "Event approved and published.");
        } catch (BusinessRuleException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/dashboard/admin";
    }

    // ===== Reject: show the reason form =====
    @GetMapping("/dashboard/admin/events/{id}/reject")
    public String rejectForm(@PathVariable Long id, Model model) {
        model.addAttribute("event", eventService.getById(id));
        return "admin/reject-event";
    }

    // ===== Reject: handle the submitted reason =====
    @PostMapping("/dashboard/admin/events/{id}/reject")
    public String rejectSubmit(@PathVariable Long id,
                                @RequestParam String reason,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        try {
            eventService.rejectEvent(id, reason);
        } catch (BusinessRuleException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("event", eventService.getById(id));
            return "admin/reject-event";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Event rejected.");
        return "redirect:/dashboard/admin";
    }

    // ===== User management =====
    @GetMapping("/dashboard/admin/users")
    public String users(Model model) {
        model.addAttribute("users", userService.getAllUsers());
        return "admin/users";
    }

    @PostMapping("/dashboard/admin/users/{id}/toggle")
    public String toggleUser(@PathVariable Long id, @RequestParam boolean enabled) {
        userService.setEnabled(id, enabled);
        return "redirect:/dashboard/admin/users";
    }

    // ===== Booking overview =====
    @GetMapping("/dashboard/admin/bookings")
    public String bookings(Model model) {
        model.addAttribute("bookings", bookingService.getAllBookingsForAdmin());
        return "admin/bookings";
    }
}
