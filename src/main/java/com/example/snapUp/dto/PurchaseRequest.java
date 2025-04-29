package com.example.snapUp.dto;

import lombok.Data;

@Data
public class PurchaseRequest {
    private Long ticketId;
    private Integer quantity;
} 