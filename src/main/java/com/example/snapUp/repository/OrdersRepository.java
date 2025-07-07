package com.example.snapUp.repository;

import com.example.snapUp.entity.Orders;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrdersRepository extends JpaRepository<Orders, String> {

    @Transactional
    @Query(value = "select * from orders where status = 0 and order_Id=:id", nativeQuery = true)
    Optional<Orders> findUnpaidOrderByTicketId(@Param("id") String id);

    @Transactional
    @Query(value = "select * from orders where order_Id=:id", nativeQuery = true)
    Optional<Orders> findByOrderId(@Param("id") String id);

    @Transactional
    @Query(value = "select * from orders where customer_id=:cusId and order_Id=:orderId", nativeQuery = true)
    Optional<Orders> findByCustomerIdAndOrderId(@Param("cusId") String cusId, @Param("orderId") Long orderId);

    @Transactional
    @Query(value = "select * from orders where customer_id=:id", nativeQuery = true)
    Optional<Orders> getTicketIdByCustomerId(@Param("id") String id);
}