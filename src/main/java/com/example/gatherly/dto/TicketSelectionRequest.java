package com.example.gatherly.dto;

/** Holds one ticket type identifier and the quantity requested for it. */
public class TicketSelectionRequest {

    private Long ticketTypeId;
    private int quantity;

    public Long getTicketTypeId() { return ticketTypeId; }
    public void setTicketTypeId(Long ticketTypeId) { this.ticketTypeId = ticketTypeId; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}
