package com.example.snapUp.controller;

import com.example.snapUp.dto.PurchaseRequest;
import com.example.snapUp.entity.Ticket;
import com.example.snapUp.repository.TicketRepository;

import java.util.Optional;
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

    @PostMapping("/reset")
    @Transactional
    public String resetTickets() {
        try {
            logger.info("Attempting to reset tickets");
            Optional<Ticket> optionalTicket = ticketRepository.findById(1L);

            Ticket ticket = optionalTicket.get();
            ticket.setType("A");
            ticket.setStock(20);
            ticketRepository.save(ticket);
            logger.info("Tickets reset successfully");
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
    public String purchaseTicket(@RequestBody PurchaseRequest request) {
        try {
            logger.info("Attempting to purchase ticket with request: {}", request);
            Optional<Ticket> optionalTicket = ticketRepository.findById(request.getTicketId());
            if (optionalTicket.isPresent()) {
                Ticket ticket = optionalTicket.get();
                int quantity = request.getQuantity() != null ? request.getQuantity() : 1;
                
                if (ticket.getStock() >= quantity) {
                    ticket.setStock(ticket.getStock() - quantity);
                    ticketRepository.save(ticket);
                    logger.info("Ticket purchased successfully. Remaining stock: {}", ticket.getStock());
                    return "購票成功！剩餘票券：" + ticket.getStock();
                } else {
                    logger.warn("Not enough tickets available. Requested: {}, Available: {}", quantity, ticket.getStock());
                    return "票券不足！剩餘票券：" + ticket.getStock();
                }
            }
            logger.warn("No ticket found with ID: {}", request.getTicketId());
            return "找不到票券！";
        } catch (Exception e) {
            logger.error("Error purchasing ticket", e);
            return "購票時發生錯誤：" + e.getMessage();
        }
    }
}