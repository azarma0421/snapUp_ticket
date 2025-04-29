package com.example.snapUp.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class Ticket {
    @Id
    private long id;
    private String name;
    private double price;
    private int stock;
    private String type;
    private int available;
} 