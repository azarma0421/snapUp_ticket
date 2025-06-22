package com.example.snapUp.service;

import com.example.snapUp.entity.TicketOrder;
import com.example.snapUp.entity.Ticket;
import com.example.snapUp.repository.TicketOrderRepository;
import com.example.snapUp.repository.TicketRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
    private TicketOrderRepository orderRepository;

    // 鎖定key
    private final String lockKey = "lock:ticket:";

    // 快取key
    private final String redisKey = "ticket_stock:";

    private static String decrTicketLuaPath = "lua/decr_ticket_stock.lua";

    @Autowired
    private RedisTemplate redisTemplate;

    public void resetTickets(String ticketId) {
        logger.info("Reset tickets...");
        Optional<Ticket> optionalTicket = ticketRepository.findById(Long.valueOf(ticketId));
        int resetStock = 20;
        orderRepository.deleteAll();

        Ticket ticket = optionalTicket.get();
        ticket.setType("A");
        ticket.setStock(resetStock);
        ticketRepository.save(ticket);
        redisTemplate.opsForValue().set(redisKey + ticketId, String.valueOf(resetStock));

        logger.info("Tickets reset successfully");
    }

    public int purchaseTicket(Long ticketId, String ticketType, String customerId, int quantity) throws IOException {

        String redisKeyStock = redisKey + ticketId;

        String lockValue = UUID.randomUUID().toString();
        boolean isLocked = false;
        int maxRetries = 5;
        long retryDelayMillis = 100;

        try {
            // 重試取鎖
            for (int retryTimes = 1; retryTimes <= maxRetries; retryTimes++) {
                isLocked = lockService.lock(lockKey + ticketId, lockValue, 5000);
                if (isLocked) {
                    break;
                }
                if (retryTimes < maxRetries - 1) {
                    try {
                        Thread.sleep(retryDelayMillis);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
            if (isLocked) {
                // 初始化 Redis
                int initStock = 0;
                if (!redisTemplate.hasKey(redisKeyStock)) {
                    Optional<Ticket> optionalTicket = ticketRepository.findById(ticketId);
                    if (optionalTicket.isEmpty()) {
                        return -2;
                    }
                    String stock = String.valueOf(optionalTicket.get().getStock());
                    redisTemplate.opsForValue().set(redisKeyStock, stock);
                }

                // 執行lua
                List<String> keys = List.of(redisKeyStock);
                List<String> argv = List.of(String.valueOf(quantity), String.valueOf(initStock));
                int result = this.exeLua(keys, argv);

                if (result >= 0) {
                    ticketRepository.updateById(ticketId, result);
                    TicketOrder order = new TicketOrder(lockValue, ticketType, customerId, "0");
                    orderRepository.save(order);
                }
                return result;
            } else {
                logger.warn("購票失敗, Ticket Id: {}", ticketId);
                return -3;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (isLocked) {
                lockService.unlock(lockKey + ticketId, lockValue);
            }
        }
    }

    private int exeLua(List<String> keys, List<String> ARGV) throws IOException {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        ClassPathResource luaFile = new ClassPathResource(decrTicketLuaPath);
        String luaScript = new String(luaFile.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        script.setScriptText(luaScript);
        script.setResultType(Long.class);
        int res = -2;
        try {
            res = Math.toIntExact((Long) redisTemplate.execute(
                    script,
                    keys,
                    ARGV.toArray()));
        } catch (Exception e) {
            Throwable cause = e.getCause();
            System.out.println("Lua 發生錯誤: " + cause.getMessage());
            cause.printStackTrace();
        }
        return res;
    }
}
