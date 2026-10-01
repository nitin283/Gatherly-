package com.example.gatherly.dto;

import java.util.ArrayList;
import java.util.List;

public class BookingRequest {

    private List<TicketSelectionRequest> selections = new ArrayList<>();

    public List<TicketSelectionRequest> getSelections() { return selections; }
    public void setSelections(List<TicketSelectionRequest> selections) { this.selections = selections; }
}