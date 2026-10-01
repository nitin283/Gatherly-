package com.example.gatherly.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.gatherly.dto.CreateEventRequest;
import com.example.gatherly.dto.TicketTypeRequest;
import com.example.gatherly.model.BookingStatus;
import com.example.gatherly.model.Event;
import com.example.gatherly.model.TicketType;
import com.example.gatherly.model.TicketTypeName;
import com.example.gatherly.model.User;
import com.example.gatherly.repository.TicketTypeRepository;
import com.example.gatherly.service.BookingService;
import com.example.gatherly.service.BusinessRuleException;
import com.example.gatherly.service.EventService;

import jakarta.validation.Valid;

@Controller
public class OrganizerController {

    private final EventService eventService;
    private final TicketTypeRepository ticketTypeRepository;
    private final BookingService bookingService;

    public OrganizerController(EventService eventService,
                                TicketTypeRepository ticketTypeRepository,
                                BookingService bookingService) {
        this.eventService = eventService;
        this.ticketTypeRepository = ticketTypeRepository;
        this.bookingService = bookingService;
    }

    // ===== My Events =====
    @GetMapping("/dashboard/organizer")
    public String dashboard(@AuthenticationPrincipal User organizer, Model model) {
        List<Event> events = eventService.getEventsByOrganizer(organizer.getId());
        model.addAttribute("events", events);
        return "dashboard/organizer";
    }

    // ===== Create: show form =====
    @GetMapping("/dashboard/organizer/events/new")
    public String createForm(Model model) {
        model.addAttribute("createEventRequest", new CreateEventRequest());
        return "organizer/create-event";
    }

    // ===== Create: handle submit =====
    @PostMapping("/dashboard/organizer/events")
    public String create(@AuthenticationPrincipal User organizer,
                          @Valid @ModelAttribute("createEventRequest") CreateEventRequest request,
                          BindingResult bindingResult,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "organizer/create-event";
        }

        Event event = new Event();
        event.setTitle(request.getTitle());
        event.setDescription(request.getDescription());
        event.setCategory(request.getCategory());
        event.setVenue(request.getVenue());
        event.setEventDate(request.getEventDate());

        try {
            Event saved = eventService.createEvent(event, organizer);

            TicketType ticketType = new TicketType();
            ticketType.setTypeName(TicketTypeName.valueOf(request.getTicketTypeName()));
            ticketType.setPrice(request.getPrice());
            ticketType.setTotalQuantity(request.getQuantity());
            ticketType.setAvailableQuantity(request.getQuantity());
            saved.addTicketType(ticketType);
            ticketTypeRepository.save(ticketType);

        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", "Invalid ticket type selected.");
            return "organizer/create-event";
        }

        redirectAttributes.addFlashAttribute("successMessage", "Event submitted for admin approval.");
        return "redirect:/dashboard/organizer";
    }

    // ===== Manage one event: details, ticket types, bookings, add ticket type =====
    @GetMapping("/dashboard/organizer/events/{id}")
    public String manage(@AuthenticationPrincipal User organizer, @PathVariable Long id, Model model) {
        Event event = eventService.getById(id);
        if (!event.getOrganizer().getId().equals(organizer.getId())) {
            throw new BusinessRuleException("You can only manage your own events.");
        }

        List<TicketType> ticketTypes = ticketTypeRepository.findByEventId(id);
        List<com.example.gatherly.model.Booking> bookings = bookingService.getBookingsForEvent(id);

        BigDecimal revenue = bookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
                .map(com.example.gatherly.model.Booking::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("event", event);
        model.addAttribute("ticketTypes", ticketTypes);
        model.addAttribute("bookings", bookings);
        model.addAttribute("revenue", revenue);
        model.addAttribute("ticketTypeRequest", new TicketTypeRequest());
        model.addAttribute("canAddMore", ticketTypes.size() < 3);
        return "organizer/manage-event";
    }

    // ===== Add another ticket type =====
    @PostMapping("/dashboard/organizer/events/{id}/ticket-types")
    public String addTicketType(@AuthenticationPrincipal User organizer,
                                 @PathVariable Long id,
                                 @Valid @ModelAttribute("ticketTypeRequest") TicketTypeRequest request,
                                 BindingResult bindingResult,
                                 Model model) {
        Event event = eventService.getById(id);
        if (!event.getOrganizer().getId().equals(organizer.getId())) {
            throw new BusinessRuleException("You can only manage your own events.");
        }

        if (bindingResult.hasErrors()) {
            return reloadManagePage(event, model);
        }

        TicketTypeName typeName;
        try {
            typeName = TicketTypeName.valueOf(request.getTypeName());
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", "Invalid ticket type.");
            return reloadManagePage(event, model);
        }

        if (ticketTypeRepository.existsByEventIdAndTypeName(id, typeName)) {
            model.addAttribute("errorMessage", "This event already has a " + typeName + " ticket type.");
            return reloadManagePage(event, model);
        }

        TicketType ticketType = new TicketType();
        ticketType.setTypeName(typeName);
        ticketType.setPrice(request.getPrice());
        ticketType.setTotalQuantity(request.getQuantity());
        ticketType.setAvailableQuantity(request.getQuantity());
        event.addTicketType(ticketType);
        ticketTypeRepository.save(ticketType);

        return "redirect:/dashboard/organizer/events/" + id;
    }

    private String reloadManagePage(Event event, Model model) {
        List<TicketType> ticketTypes = ticketTypeRepository.findByEventId(event.getId());
        List<com.example.gatherly.model.Booking> bookings = bookingService.getBookingsForEvent(event.getId());
        BigDecimal revenue = bookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
                .map(com.example.gatherly.model.Booking::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        model.addAttribute("event", event);
        model.addAttribute("ticketTypes", ticketTypes);
        model.addAttribute("bookings", bookings);
        model.addAttribute("revenue", revenue);
        model.addAttribute("canAddMore", ticketTypes.size() < 3);
        return "organizer/manage-event";
    }

    // ===== Edit: show form =====
    @GetMapping("/dashboard/organizer/events/{id}/edit")
    public String editForm(@AuthenticationPrincipal User organizer, @PathVariable Long id, Model model) {
        Event event = eventService.getById(id);
        if (!event.getOrganizer().getId().equals(organizer.getId())) {
            throw new BusinessRuleException("You can only edit your own events.");
        }
        model.addAttribute("event", event);
        return "organizer/edit-event";
    }

    // ===== Edit: handle submit =====
    @PostMapping("/dashboard/organizer/events/{id}/edit")
    public String edit(@AuthenticationPrincipal User organizer,
                        @PathVariable Long id,
                        @ModelAttribute Event formEvent,
                        Model model,
                        RedirectAttributes redirectAttributes) {
        try {
            eventService.updateEvent(id, organizer.getId(), formEvent);
        } catch (BusinessRuleException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("event", formEvent);
            return "organizer/edit-event";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Event updated and sent for approval again.");
        return "redirect:/dashboard/organizer";
    }

    // ===== Delete: confirm page =====
    @GetMapping("/dashboard/organizer/events/{id}/delete")
    public String deleteConfirm(@AuthenticationPrincipal User organizer, @PathVariable Long id, Model model) {
        Event event = eventService.getById(id);
        if (!event.getOrganizer().getId().equals(organizer.getId())) {
            throw new BusinessRuleException("You can only delete your own events.");
        }
        model.addAttribute("event", event);
        return "organizer/delete-confirm";
    }

    // ===== Delete: handle submit =====
    @PostMapping("/dashboard/organizer/events/{id}/delete")
    public String delete(@AuthenticationPrincipal User organizer, @PathVariable Long id,
                         RedirectAttributes redirectAttributes) {
        try {
            eventService.deleteEvent(id, organizer.getId());
        } catch (BusinessRuleException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/dashboard/organizer";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Event deleted.");
        return "redirect:/dashboard/organizer";
    }
}
