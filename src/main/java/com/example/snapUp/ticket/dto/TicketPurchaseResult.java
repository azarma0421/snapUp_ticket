package com.example.snapUp.ticket.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TicketPurchaseResult {
    public enum Status {
        SUCCESS,
        OUT_OF_STOCK,
        EXE_LUA_FAILED,
        RUN_TIME_OUT
    }

    private final Status status;
    private final int remainingTickets; // only meaningful when SUCCESS
}