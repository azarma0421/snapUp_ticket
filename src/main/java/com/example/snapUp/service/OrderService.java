package com.example.snapUp.service;

import com.example.snapUp.entity.Orders;
import com.example.snapUp.entity.Ticket;
import com.example.snapUp.repository.OrdersRepository;
import com.example.snapUp.repository.TicketRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.Async;

import java.util.Optional;

@Service
@EnableAsync
public class OrderService {
    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    @Autowired
    private OrdersRepository ordersRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private RedissonClient redissonClient;

    public int payByCustomerId(String customerId) {
        Optional<Orders> order = ordersRepository.getTicketIdByCustomerId(customerId);
        if (!order.isPresent()) {
            return 0;
        }
        String ticketId = order.get().getOrderId();
        Optional<Orders> o = ordersRepository.findUnpaidOrderByTicketId(ticketId);
        if (o.isPresent()) {
            Orders newOrder = o.get();
            newOrder.setStatus("1");
            ordersRepository.save(newOrder);
            redissonClient.getScoredSortedSet("order:delay:queue").remove(ticketId);
            return 1;
        }
        return 0;
    }

    // TODO 目前沒用
    public int cancelByOrderId(String orderId) {
        Optional<Orders> order = ordersRepository.findUnpaidOrderByTicketId(orderId);
        if (order.isPresent()) {
            Orders newOrder = order.get();
            newOrder.setStatus("2");
            ordersRepository.save(newOrder);
            return 1;
        }
        return 0;
    }

    // 0 paying, 1 payed, 2 cancel
    public void changeOrderStatusByOrderId(String orderId, String status) {
        Optional<Orders> order = ordersRepository.findByOrderId(orderId);
        if (order.isPresent()) {
            Orders newOrder = order.get();
            newOrder.setStatus(status);
            ordersRepository.save(newOrder);
        }
    }

    public void setOrderDelay(String orderId) {
        long expireAt = System.currentTimeMillis() + 15 * 1000;
        redissonClient.getScoredSortedSet("order:delay:queue").add(expireAt, orderId);
    }

    @Async
    public void cancelOrderById(String orderId) {
        Optional<Orders> order = ordersRepository.findById(orderId);
        if (order.isPresent() && "0".equals(order.get().getStatus())) {
            Orders o = order.get();
            o.setStatus("2");
            Ticket ticket = ticketRepository.findById(o.getTicketType()).get();
            ticketRepository.updateByType(o.getTicketType(), ticket.getStock() + 1);
            ordersRepository.save(o);
        }
    }
}
