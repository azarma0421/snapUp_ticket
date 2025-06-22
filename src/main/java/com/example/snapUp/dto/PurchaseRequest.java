package com.example.snapUp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class PurchaseRequest {
    private Long ticketId;
    private String ticket_type;
    private String customer_id;
    private Integer quantity;
} 