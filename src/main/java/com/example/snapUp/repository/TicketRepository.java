package com.example.snapUp.repository;

import com.example.snapUp.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    Optional<Ticket> findByType(String string);
} 