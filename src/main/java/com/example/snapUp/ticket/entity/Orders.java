package com.example.snapUp.ticket.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Orders {

    //    票券ID
    @Id
    private String orderId;

    //    票券類型
    private String ticketType;

    //    買家ID
    private String customerId;

    //    付款狀態
    private String status;
}