package com.example.snapUp.ticket.services;

import com.example.snapUp.common.services.LockService;
import com.example.snapUp.common.services.LuaScriptService;
import com.example.snapUp.ticket.enums.OrderStatus;
import com.example.snapUp.common.constant.TicketConstants;
import com.example.snapUp.ticket.dto.TicketPurchaseResult;
import com.example.snapUp.ticket.entity.Orders;
import com.example.snapUp.ticket.entity.Ticket;
import com.example.snapUp.ticket.repository.OrdersRepository;
import com.example.snapUp.ticket.repository.TicketRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

import com.example.snapUp.common.exceptions.DataNotFoundException;

@Service
public class TicketService {
    private static final Logger logger = LoggerFactory.getLogger(TicketService.class);

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private LockService lockService;

    @Autowired
    private OrdersRepository ordersRepository;

    @Autowired
    private OrderService orderService;

    @Autowired
    private LuaScriptService luaScriptService;

    private static final int DEFAULT_RESET_STOCK = 20;

    private static final String DECR_TICKET_LUA_PATH = "lua/decr_ticket_stock.lua";

    @Autowired
    private RedisTemplate redisTemplate;

    // 重設所有資料
    public void resetTickets(String ticketType) {
        logger.info("Reset tickets...");
        Optional<Ticket> optionalTicket = ticketRepository.findById(ticketType);
        ordersRepository.deleteAll();

        Ticket ticket = optionalTicket.get();
        ticket.setStock(DEFAULT_RESET_STOCK);
        ticketRepository.save(ticket);

        resetRedisStock(TicketConstants.TICKET_STOCK_KEY_PREFIX + ticketType, DEFAULT_RESET_STOCK);

        logger.info("Tickets reset successfully");
    }

    private void resetRedisStock(String redisKey, int stock) {
        redisTemplate.execute((RedisCallback<Object>) connection -> {
            connection.flushAll();
            return null;
        });
        redisTemplate.opsForValue().set(redisKey, String.valueOf(stock));
    }

    public TicketPurchaseResult purchaseTicket(String ticketType, String customerId, int quantity)
            throws IOException {

        String redisKeyStock = TicketConstants.TICKET_STOCK_KEY_PREFIX + ticketType;

        return lockService.executeWithLock(
                TicketConstants.TICKET_LOCK_KEY_PREFIX + ticketType,
                TicketConstants.MAX_LOCK_RETRIES,
                TicketConstants.LOCK_RETRY_DELAY_MS, () -> {
                    initRedisIfAbsent(redisKeyStock, ticketType);

                    // 執行lua
                    int result = exePurchaseTicketLua(redisKeyStock, quantity);
                    if (result >= 0) {
                        // update to db
                        ticketRepository.updateByType(ticketType, result);
                        createOrder(ticketType, customerId);
                    }

                    return switch (result) {
                        case -1 -> new TicketPurchaseResult(TicketPurchaseResult.Status.OUT_OF_STOCK, 0);
                        case -2 -> new TicketPurchaseResult(TicketPurchaseResult.Status.EXE_LUA_FAILED, 0);
                        case -3 -> new TicketPurchaseResult(TicketPurchaseResult.Status.RUN_TIME_OUT, 0);
                        default -> new TicketPurchaseResult(TicketPurchaseResult.Status.SUCCESS, result);
                    };
                });
    }

    private void createOrder(String ticketType, String customerId) {
        String orderId = UUID.randomUUID().toString();
        Orders order = new Orders(orderId, ticketType, customerId, OrderStatus.CANCELLED.getCode());
        ordersRepository.save(order);
        orderService.setOrderDelay(orderId);
    }

    // If redis don't contain this key, set it from db
    private void initRedisIfAbsent(String redisKeyStock, String ticketType) {
        if (!redisTemplate.hasKey(redisKeyStock)) {
            Optional<Ticket> optionalTicket = ticketRepository.findById(ticketType);
            if (optionalTicket.isEmpty()) {
                throw new DataNotFoundException("TicketType: " + ticketType + " not found.");
            }
            String stock = String.valueOf(optionalTicket.get().getStock());
            redisTemplate.opsForValue().set(redisKeyStock, stock);
        }
    }

    private int exePurchaseTicketLua(String redisKeyStock, int purchaseQuantity) throws IOException {
        List<String> keys = List.of(redisKeyStock);
        List<String> argv = List.of(String.valueOf(purchaseQuantity), String.valueOf(TicketConstants.INIT_REDIS_STOCK_IF_NOT_EXT));
        return luaScriptService.exeLua(keys, argv, DECR_TICKET_LUA_PATH);
    }
}