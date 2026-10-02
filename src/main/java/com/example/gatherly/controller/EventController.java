package com.example.gatherly.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.gatherly.dto.BookingRequest;
import com.example.gatherly.dto.TicketSelectionRequest;
import com.example.gatherly.model.Event;
import com.example.gatherly.model.TicketType;
import com.example.gatherly.repository.TicketTypeRepository;
import com.example.gatherly.service.EventService;

/** Handles public event browsing, search, filtering, and event detail pages. */
@Controller
public class EventController {

    private final EventService eventService;
    private final TicketTypeRepository ticketTypeRepository;

    public EventController(EventService eventService, TicketTypeRepository ticketTypeRepository) {
        this.eventService = eventService;
        this.ticketTypeRepository = ticketTypeRepository;
    }

    @GetMapping("/events")
    public String list(@RequestParam(required = false) String title,
                        @RequestParam(required = false) String category,
                        Model model) {
        List<Event> events;
        if (title != null && !title.isBlank()) {
            events = eventService.searchApprovedByTitle(title);
        } else if (category != null && !category.isBlank()) {
            events = eventService.filterApprovedByCategory(category);
        } else {
            events = eventService.getApprovedEvents();
        }
        model.addAttribute("events", events);
        model.addAttribute("title", title);
        model.addAttribute("category", category);
        return "events/list";
    }

    @GetMapping("/events/{id}")
    public String details(@PathVariable Long id, Model model) {
        Event event = eventService.getById(id);
        List<TicketType> ticketTypes = ticketTypeRepository.findByEventId(id);

        BookingRequest bookingRequest = new BookingRequest();
        for (TicketType tt : ticketTypes) {
            TicketSelectionRequest selection = new TicketSelectionRequest();
            selection.setTicketTypeId(tt.getId());
            selection.setQuantity(0);
            bookingRequest.getSelections().add(selection);
        }

        model.addAttribute("event", event);
        model.addAttribute("ticketTypes", ticketTypes);
        model.addAttribute("bookingRequest", bookingRequest);
        return "events/details";
    }
}
