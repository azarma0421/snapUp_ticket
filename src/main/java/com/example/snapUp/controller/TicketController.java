package com.example.snapUp.controller;

import com.example.snapUp.entity.Ticket;
import com.example.snapUp.repository.TicketRepository;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    @Autowired
    private TicketRepository ticketRepository;

    @PostMapping("/reset")
    @Transactional
    public String resetTickets() {
        Optional<Ticket> optionalTicket = ticketRepository.findById(1L);

        if (optionalTicket.isPresent()) {
            Ticket ticket = optionalTicket.get();
            ticket.setType("A");
            ticket.setAmount(50);
            ticketRepository.save(ticket);
            return "票券已重設！";
        }
        return "找不到票券！";
    }
}