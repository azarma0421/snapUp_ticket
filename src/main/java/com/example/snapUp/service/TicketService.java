package com.example.snapUp.service;

import com.example.snapUp.controller.TicketController;
import com.example.snapUp.entity.Ticket;
import com.example.snapUp.repository.TicketRepository;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
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
import java.util.concurrent.TimeUnit;

@Service
public class TicketService {
    private static final Logger logger = LoggerFactory.getLogger(TicketController.class);

    private final RedissonClient redissonClient;

    @Autowired
    private TicketRepository ticketRepository;

    // 鎖定key
    private final String lockKey = "lock:ticket:";

    // 快取key
    private final String redisKey = "ticket_stock:";

    private static String luaPath = "lua/decr_ticket_stock.lua";

    @Autowired
    private RedisTemplate redisTemplate;

    public TicketService(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    public void resetTickets() {
        logger.info("Reset tickets...");
        Optional<Ticket> optionalTicket = ticketRepository.findById(1L);
        int resetStock = 20;

        Ticket ticket = optionalTicket.get();
        ticket.setType("A");
        ticket.setStock(resetStock);
        ticketRepository.save(ticket);
        redisTemplate.opsForValue().set(redisKey + "1", String.valueOf(resetStock));

        logger.info("Tickets reset successfully");
    }

    public int purchaseTicket(Long ticketId, int quantity) {

        String redisKeyStock = redisKey + ticketId;

        RLock lock = redissonClient.getLock(lockKey + ticketId);
        try {
            boolean isLocked = lock.tryLock(3, 5, TimeUnit.SECONDS);
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
                }
                return result;
            } else {
                return -3;
            }
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }

    private int exeLua(List<String> keys, List<String> ARGV) throws IOException {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        ClassPathResource luaFile = new ClassPathResource(luaPath);
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
