package com.example.snapUp.repository;

import com.example.snapUp.entity.TicketOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TicketOrderRepository extends JpaRepository<TicketOrder, String> {

    @Transactional
    @Query(value = "select * from ticket_order where status = 0 and ticket_id=:id", nativeQuery = true)
    Optional<TicketOrder> findUnpaidOrderByTicketId(@Param("id") String id);

    @Transactional
    @Query(value = "select * from ticket_order where ticket_id=:id", nativeQuery = true)
    Optional<TicketOrder> findByTicketId(@Param("id") String id);

    @Transactional
    @Query(value = "select * from ticket_order where customer_id=:cusId and ticket_id=:ticketId", nativeQuery = true)
    Optional<TicketOrder> findByCustomerIdAndTicketId(@Param("cusId") String cusId, @Param("ticketId") Long ticketId);

    @Transactional
    @Query(value = "select * from ticket_order where customer_id=:id", nativeQuery = true)
    Optional<TicketOrder> getTicketIdByCustomerId(@Param("id") String id);
}