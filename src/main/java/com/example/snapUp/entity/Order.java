package com.example.snapUp.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class Order {
    @Id
    private long ticket_id;
    private String ticket_type;
    private String customer_id;
    private int status;
}