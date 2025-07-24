package com.example.snapUp.ticket.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class Ticket {
    @Id
    private String type;
    private String name;
    private double price;
    private int stock;
    private int available;
} 