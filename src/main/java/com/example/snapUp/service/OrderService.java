package com.example.snapUp.service;

import com.example.snapUp.entity.TicketOrder;
import com.example.snapUp.repository.TicketOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class OrderService {
    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    @Autowired
    private TicketOrderRepository orderRepository;

    public int payByCustomerId(String customerId) {
        Optional<TicketOrder> order = orderRepository.findPayingOrderByCustomer_id(customerId);
        if (order.isPresent()) {
            TicketOrder newOrder = order.get();
            newOrder.setStatus("1");
            orderRepository.save(newOrder);
            return 1;
        }
        return 0;
    }

    public int cancelByCustomerId(String customerId) {
        Optional<TicketOrder> order = orderRepository.findPayingOrderByCustomer_id(customerId);
        if (order.isPresent()) {
            TicketOrder newOrder = order.get();
            newOrder.setStatus("2");
            orderRepository.save(newOrder);
            return 1;
        }
        return 0;
    }

    // 0 paying, 1 payed, 2 cancel
    public void changeOrderStatusByCustomerId(String customerId, String status) {
        Optional<TicketOrder> order = orderRepository.findByCustomer_id(customerId);
        if (order.isPresent()) {
            TicketOrder newOrder = order.get();
            newOrder.setStatus(status);
            orderRepository.save(newOrder);
        }
    }
}
