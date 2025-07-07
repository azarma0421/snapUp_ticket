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

    // TODO ticketId 要來自前端
    @PostMapping("/reset")
    @Transactional
    public String resetTickets(@RequestParam(defaultValue = "A") String  ticketType) {
        try {
            ticketService.resetTickets(ticketType);
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
            Optional<Ticket> optionalTicket = ticketRepository.findById("A");
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
    public int purchaseTicket(@RequestBody PurchaseRequest request) throws IOException {
        int quantity = request.getQuantity() != null ? request.getQuantity() : 1;
        // TODO ticketType、customerId 要來自前端，先寫死
        return ticketService.purchaseTicket(request.getTicketType(), request.getCustomerId(), quantity);
    }
    }
}