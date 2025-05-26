package com.example.snapUp.controller;

import com.example.snapUp.dto.PurchaseRequest;
import com.example.snapUp.entity.Ticket;
import com.example.snapUp.repository.TicketRepository;

import java.io.IOException;
import java.util.Optional;

import com.example.snapUp.service.TicketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {
    private static final Logger logger = LoggerFactory.getLogger(TicketController.class);

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private TicketService ticketService;

    @PostMapping("/reset")
    @Transactional
    public String resetTickets() {
        try {
            ticketService.resetTickets();
            return "票券已重設！";
        } catch (Exception e) {
            logger.error("Error resetting tickets", e);
            return "重設票券時發生錯誤：" + e.getMessage();
        }
    }

    @PostMapping("/showRemain")
    public String showRemain() {
        try {
            logger.info("Attempting to show remaining tickets");
            Optional<Ticket> optionalTicket = ticketRepository.findById(1L);
            if (optionalTicket.isPresent()) {
                Ticket ticket = optionalTicket.get();
                logger.info("Found ticket with stock: {}", ticket.getStock());
                return String.valueOf(ticket.getStock());
            }
            logger.warn("No ticket found with ID 1");
            return "找不到票券！";
        } catch (Exception e) {
            logger.error("Error showing remaining tickets", e);
            return "查詢剩餘票券時發生錯誤：" + e.getMessage();
        }
    }

    @PostMapping("/purchase")
    @Transactional
    public String purchaseTicket(@RequestBody PurchaseRequest request) throws IOException {
        Long ticketId = request.getTicketId();
        int quantity = request.getQuantity() != null ? request.getQuantity() : 1;
        int result = ticketService.purchaseTicket(ticketId, quantity);

        if (result == -3) {
            return "系統繁忙中";
        } else if (result == -2) {
            return "Lua 發生錯誤";
        } else if (result == -1) {
            return "購票失敗：票券不足";
        } else {
            return "購票成功！剩餘票券：" + result;
        }
    }
}