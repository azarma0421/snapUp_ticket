package com.example.snapUp.repository;

import com.example.snapUp.entity.Ticket;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    @Modifying
    @Transactional
    @Query("update Ticket t set t.stock = :stock where t.id = :id")
    void updateById(@Param("id") long id, @Param("stock") int stock);
}