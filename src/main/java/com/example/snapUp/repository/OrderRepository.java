package com.example.snapUp.repository;

import com.example.snapUp.entity.Order;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Modifying
    @Transactional
    @Query("insert into `order` values(:ticketId,:ticketType,:customerId,:status)")
    void insertOrder(@Param("ticketId") long ticketId,
                     @Param("ticketType") String ticketType,
                     @Param("customerId") String customerId,
                     @Param("status") String status);

    @Modifying
    @Transactional
    @Query("delete from `order`")
    void refreshOrders();
}