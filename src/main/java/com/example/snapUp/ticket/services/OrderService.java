package com.example.snapUp.ticket.services;

import com.example.snapUp.common.constant.RedisConstants;
import com.example.snapUp.common.constant.TicketConstants;
import com.example.snapUp.common.services.LockService;
import com.example.snapUp.common.services.LuaScriptService;
import com.example.snapUp.ticket.entity.Orders;
import com.example.snapUp.ticket.entity.Ticket;
import com.example.snapUp.ticket.enums.OrderStatus;
import com.example.snapUp.ticket.repository.OrdersRepository;
import com.example.snapUp.ticket.repository.TicketRepository;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.zip.DataFormatException;

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

    private static String incrTicketLuaPath = "lua/incr_ticket_stock.lua";

    public void payByCustomerId(String customerId) throws DataFormatException {
        Optional<Orders> order = ordersRepository.getTicketIdByCustomerId(customerId);
        if (order.isEmpty()) {
            throw new DataFormatException("Customer order not found, customerId: " + customerId);
        }
        confirmOrder(order.get().getOrderId());
    }

    private void confirmOrder(String orderId) throws DataFormatException {
        Optional<Orders> o = ordersRepository.findUnpaidOrderByOrderId(orderId);
        if (o.isPresent()) {
            Orders newOrder = o.get();
            newOrder.setStatus(OrderStatus.CONFIRMED.getCode());
            ordersRepository.save(newOrder);
            redissonClient.getScoredSortedSet(RedisConstants.ORDER_DELAY_QUEUE).remove(orderId);
        } else {
            throw new DataFormatException("Order has payed, orderId: " + orderId);
        }
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
        redissonClient.getScoredSortedSet(RedisConstants.ORDER_DELAY_QUEUE).add(expireAt, orderId);
    }

    @Async
    public CompletableFuture<Boolean> cancelOrderById(String orderId) {

        Optional<Orders> order = ordersRepository.findById(orderId);
        if (order.isEmpty()) {
            logger.warn("Cancel failed: Order not found, orderId: {}", orderId);
            return CompletableFuture.completedFuture(false);
        }

        if (!order.get().getStatus().equals(OrderStatus.PENDING.getCode())) {
            logger.warn("Cancel failed: Order not pending, orderId: {}", orderId);
            return CompletableFuture.completedFuture(false);
        }
        Orders o = order.get();
        o.setStatus(OrderStatus.CANCELLED.getCode());

        try {
            boolean success = lockService.executeWithLock(
                    RedisConstants.TICKET_LOCK_KEY_PREFIX + o.getTicketType(),
                    TicketConstants.MAX_LOCK_RETRIES,
                    TicketConstants.LOCK_RETRY_DELAY_MS, () -> {
                        try {
                            updateDbAndRedis(o);
                            logger.info("Order cancelled successfully: {}", orderId);
                            return true;
                        } catch (Exception e) {
                            logger.error("cancelOrderById error: ", e);
                            return false;
                        }
                    });
            return CompletableFuture.completedFuture(success);
        } catch (IOException e) {
            logger.error("Unexpected error when trying to cancel orderId: {}", orderId, e);
            return CompletableFuture.completedFuture(false);
        }
    };

    private void updateDbAndRedis(Orders o) throws IOException {
        String ticketType = o.getTicketType();
        // 1. update DB
        Ticket ticket = ticketRepository.findById(ticketType).get();
        ticketRepository.updateByType(ticketType, ticket.getStock() + 1);
        ordersRepository.save(o);

        // 2. update redis
        List<String> keys = List.of(RedisConstants.TICKET_STOCK_KEY_PREFIX + ticketType);
        List<String> argv = List.of(String.valueOf(1), String.valueOf(ticket.getStock()));
        luaScriptService.exeLua(keys, argv, incrTicketLuaPath);
    }
}
