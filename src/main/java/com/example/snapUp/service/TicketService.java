package com.example.snapUp.service;

import com.example.snapUp.entity.Orders;
import com.example.snapUp.entity.Ticket;
import com.example.snapUp.repository.OrdersRepository;
import com.example.snapUp.repository.TicketRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

    // 鎖定key
    private final String lockKey = "lock:ticket:";

    // 快取key
    private final String redisKey = "ticket_stock:";

    private static String decrTicketLuaPath = "lua/decr_ticket_stock.lua";

    @Autowired
    private RedisTemplate redisTemplate;

    // 重設所有資料
    public void resetTickets(String ticketType) {
        logger.info("Reset tickets...");
        Optional<Ticket> optionalTicket = ticketRepository.findById(ticketType);
        int resetStock = 20;
        ordersRepository.deleteAll();

        Ticket ticket = optionalTicket.get();
        ticket.setStock(resetStock);
        ticketRepository.save(ticket);

        redisTemplate.execute((RedisCallback<Object>) connection -> {
            connection.flushAll();
            return null;
        });
        redisTemplate.opsForValue().set(redisKey + ticketType, String.valueOf(resetStock));
        
        logger.info("Tickets reset successfully");
    }

    public int purchaseTicket(String ticketType, String customerId, int quantity) throws IOException {

        String redisKeyStock = redisKey + ticketType;

        String lockValue = UUID.randomUUID().toString();
        boolean isLocked = false;
        int maxRetries = 5;
        long retryDelayMillis = 100;

        try {
            isLocked = lockService.Lock(lockKey + ticketType, lockValue, maxRetries, retryDelayMillis);
            if (isLocked) {
                // 初始化 Redis
                int initStock = 0;
                if (!redisTemplate.hasKey(redisKeyStock)) {
                    Optional<Ticket> optionalTicket = ticketRepository.findById(ticketType);
                    if (optionalTicket.isEmpty()) {
                        return -2;
                    }
                    String stock = String.valueOf(optionalTicket.get().getStock());
                    redisTemplate.opsForValue().set(redisKeyStock, stock);
                }

                // 執行lua
                List<String> keys = List.of(redisKeyStock);
                List<String> argv = List.of(String.valueOf(quantity), String.valueOf(initStock));
                int result = luaScriptService.exeLua(keys, argv, decrTicketLuaPath);

                if (result >= 0) {
                    ticketRepository.updateByType(ticketType, result);
                    Orders order = new Orders(lockValue, ticketType, customerId, "0");
                    ordersRepository.save(order);
                    orderService.setOrderDelay(lockValue);
                }
                return result;
            } else {
                logger.warn("購票失敗, Ticket Type: {}", ticketType);
                return -3;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (isLocked) {
                lockService.unlock(lockKey + ticketType, lockValue);
            }
        }
    }
}