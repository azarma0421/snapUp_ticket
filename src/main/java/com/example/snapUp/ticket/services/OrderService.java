package com.example.snapUp.ticket.services;

import com.example.snapUp.common.services.LockService;
import com.example.snapUp.common.services.LuaScriptService;
import com.example.snapUp.ticket.entity.Orders;
import com.example.snapUp.ticket.entity.Ticket;
import com.example.snapUp.ticket.repository.OrdersRepository;
import com.example.snapUp.ticket.repository.TicketRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.Async;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

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

    @Autowired
    private LockService lockService;

    @Autowired
    private LuaScriptService luaScriptService;

    private final String cancelKey = "cancel_order:";

    private final String redisKey = "ticket_stock:";

    private static String incrTicketLuaPath = "lua/incr_ticket_stock.lua";

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
    public CompletableFuture<Boolean> cancelOrderById(String orderId) {
        String lockValue = UUID.randomUUID().toString();
        boolean isLocked = false;

        Optional<Orders> order = ordersRepository.findById(orderId);
        if (order.isPresent() && "0".equals(order.get().getStatus())) {
            Orders o = order.get();
            o.setStatus("2");

            try {
                isLocked = lockService.Lock(cancelKey + o.getTicketType(), lockValue, 5, 100);

                if (isLocked) {

                    // 1. update DB
                    Ticket ticket = ticketRepository.findById(o.getTicketType()).get();
                    ticketRepository.updateByType(o.getTicketType(), ticket.getStock() + 1);
                    ordersRepository.save(o);

                    // 2. update redis
                    List<String> keys = List.of(redisKey + o.getTicketType());
                    List<String> argv = List.of(String.valueOf(1), String.valueOf(ticket.getStock()));
                    luaScriptService.exeLua(keys, argv, incrTicketLuaPath);

                } else {
                    logger.info("orderId: {} get lock fail.", orderId);
                    return CompletableFuture.completedFuture(false);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (Exception e) {
                logger.error("cancelOrderById error: ", e);
            } finally {
                if (isLocked) {
                    try {
                        lockService.unlock(cancelKey + o.getTicketType(), lockValue);
                    } catch (IOException e) {
                        logger.error("Unlock failed: ", e);
                    }
                }
            }
        } else {
            logger.error("orderId not found: ", orderId);
            return CompletableFuture.completedFuture(false);
        }
        return CompletableFuture.completedFuture(true);
    }
}
