package com.example.snapUp.repository;

import com.example.snapUp.entity.TicketOrder;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface TicketOrderRepository extends JpaRepository<TicketOrder, Long> {

    @Transactional
    @Query("select * from ticket_order where status = 0 and customer_id=:id")
    Optional<TicketOrder> findPayingOrderByCustomer_id(@Param("id") String id);

    @Transactional
    @Query("select * from ticket_order where customer_id=:id")
    Optional<TicketOrder> findByCustomer_id(@Param("id") String id);
}