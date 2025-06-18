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
    private String ticket_id;

    //    票券類型
    private String ticket_type;

    //    買家ID
    private String customer_id;

    //    付款狀態
    private String status;
}