package com.example.snapUp.service;

import com.example.snapUp.entity.TicketOrder;
import com.example.snapUp.repository.TicketOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.redisson.api.RedissonClient;

import java.util.Optional;

@Service
public class OrderService {
    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    @Autowired
    private TicketOrderRepository orderRepository;

    @Autowired
    private RedissonClient redissonClient;

    public int payByTicketId(String customerId) {
        Optional<TicketOrder> order = orderRepository.getTicketIdByCustomerId(customerId);
        if (!order.isPresent()) {
            return 0;
        }
        String ticketId = order.get().getTicketId();
        Optional<TicketOrder> o = orderRepository.findUnpaidOrderByTicketId(ticketId);
        if (o.isPresent()) {
            TicketOrder newOrder = o.get();
            newOrder.setStatus("1");
            orderRepository.save(newOrder);
            redissonClient.getScoredSortedSet("order:delay:queue").remove(ticketId);
            return 1;
        }
        return 0;
    }

    public int cancelByTicketId(String ticketId) {
        Optional<TicketOrder> order = orderRepository.findUnpaidOrderByTicketId(ticketId);
        if (order.isPresent()) {
            TicketOrder newOrder = order.get();
            newOrder.setStatus("2");
            orderRepository.save(newOrder);
            return 1;
        }
        return 0;
    }

    // 0 paying, 1 payed, 2 cancel
    public void changeOrderStatusByTicketId(String ticketId, String status) {
        Optional<TicketOrder> order = orderRepository.findByTicketId(ticketId);
        if (order.isPresent()) {
            TicketOrder newOrder = order.get();
            newOrder.setStatus(status);
            orderRepository.save(newOrder);
        }
    }

    public void setOrderDelay(String ticketId) {
        long expireAt = System.currentTimeMillis() + 15 * 1000;
        redissonClient.getScoredSortedSet("order:delay:queue").add(expireAt, ticketId);
    }

    public void cancelOrderById(String ticketId) {
        Optional<TicketOrder> order = orderRepository.findById(ticketId);
        if (order.isPresent() && "0".equals(order.get().getStatus())) {
            TicketOrder o = order.get();
            o.setStatus("2");
            orderRepository.save(o);
        }
    }
}
