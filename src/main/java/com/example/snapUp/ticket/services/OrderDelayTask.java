package com.example.snapUp.ticket.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.redisson.api.RedissonClient;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.redisson.api.RScoredSortedSet;

import java.io.IOException;
import java.util.Collection;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;

@Component
public class OrderDelayTask {
    private static final Logger logger = LoggerFactory.getLogger(OrderDelayTask.class);

    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private OrderService orderService; // 用來改訂單狀態

    @Scheduled(fixedRate = 5000) // 每5秒執行一次
    public void cancelExpiredOrders() throws IOException {
        long now = System.currentTimeMillis();
        RScoredSortedSet<String> zset = redissonClient.getScoredSortedSet("order:delay:queue");
        Collection<String> expiredOrderIds = zset.valueRange(0, true, now, true);

        for (String orderId : expiredOrderIds) {
            orderService.cancelOrderById(orderId).thenAccept(result -> {
                if (result) {
                    zset.remove(orderId);
                } else {
                    logger.error("order cancel fail, orderId: {}", orderId);
                }
            });
        }
    }
}
