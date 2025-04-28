package com.example.snapUp.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class Ticket {
    @Id
    private long id;
    private String type;
    private int amount;
} 