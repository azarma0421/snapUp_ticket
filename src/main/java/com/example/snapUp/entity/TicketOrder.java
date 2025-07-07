package com.example.snapUp.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TicketOrder {

    //    票券ID
    @Id
    private String ticketId;

    //    票券類型
    private String ticketType;

    //    買家ID
    private String customerId;

    //    付款狀態
    private String status;
}