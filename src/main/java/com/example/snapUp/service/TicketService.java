package com.example.snapUp.service;

import com.example.snapUp.entity.Ticket;
import com.example.snapUp.repository.TicketRepository;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
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
    private final RedissonClient redissonClient;

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
                    initStock = optionalTicket.get().getStock();
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
                return -1;
            }
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private int exeLua(List<String> keys, List<String> ARGV) throws IOException {
        DefaultRedisScript<Integer> script = new DefaultRedisScript<>();
        ClassPathResource luaFile = new ClassPathResource(luaPath);
        String luaScript = new String(luaFile.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        script.setScriptText(luaScript);
        script.setResultType(Integer.class);
        int res = -2;
        try {
            res = (int) redisTemplate.execute(
                    script,
                    keys,
                    ARGV.toArray());
        } catch (Exception e) {
            Throwable cause = e.getCause();
            System.out.println("Lua 發生錯誤: " + cause.getMessage());
            cause.printStackTrace();
        }
        return res;
    }
}
