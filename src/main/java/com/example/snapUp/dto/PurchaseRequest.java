package com.example.snapUp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class PurchaseRequest {
    private String ticketId;
    private String ticketType;
    private String customerId;
    private Integer quantity;
} 