package com.example.snapUp;

import com.example.snapUp.repository.OrderRepository;
import com.example.snapUp.repository.TicketRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TicketConcurrencyTest {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private OrderRepository orderRepository;

    private static final String luaPath = "lua/decr_ticket_stock.lua";
    private static final String stockKey = "ticket_stock:1";

    @Test
    public void testHighConcurrencyBuying() throws InterruptedException {
        System.out.println(">>> TicketConcurrencyTest 正在執行");
        final int threadCount = 50;
        final int initTicket = 20;
        AtomicInteger successCount = new AtomicInteger();
        orderRepository.refreshOrders();
        CountDownLatch latch = new CountDownLatch(threadCount);

        redisTemplate.opsForValue().set(stockKey, String.valueOf(initTicket));

        for (int i = 0; i < threadCount; i++) {
            int userId = i + 1;
            new Thread(() -> {
                try {
                    List<String> keys = List.of(stockKey);
                    List<String> argv = List.of("1", String.valueOf(initTicket));
                    Long res = exeLua(keys, argv);

                    if (res == -2) {
                        System.out.println("Lua 發生錯誤");
                    } else if (res == -1) {
                        System.out.println("用戶 " + userId + " 購票失敗：票券不足");
                    } else {
                        // TODO 持久化 ticket、order
                        successCount.getAndIncrement();
                        System.out.println("用戶 " + userId + " 購票成功，票券剩餘: " + res);
                    }
                } catch (Exception e) {
                    System.out.println("Lua 發生錯誤: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            }).start();
        }
        latch.await();
        System.out.println("購買成功人數: " + successCount);
        System.out.println("模擬結束");
    }

    private Long exeLua(List<String> keys, List<String> ARGV) throws IOException {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        ClassPathResource luaFile = new ClassPathResource(luaPath);
        String luaScript = new String(luaFile.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        script.setScriptText(luaScript);
        script.setResultType(Long.class);
        Long res = -2L;
        try {
            res = redisTemplate.execute(
                    script,
                    keys,
                    ARGV.toArray()
            );
        } catch (Exception e) {
            Throwable cause = e.getCause();
            System.out.println("Lua 發生錯誤: " + cause.getMessage());
            cause.printStackTrace();
        }
        return res;
    }
}
