package com.example.snapUp.service;
import org.springframework.beans.factory.annotation.Autowired;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.redisson.api.RScoredSortedSet;
import java.util.Collection;
import org.springframework.stereotype.Component;

@Component
public class OrderDelayTask {
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private OrderService orderService; // 用來改訂單狀態

    @Scheduled(fixedRate = 5000) // 每5秒執行一次
    public void cancelExpiredOrders() {
        long now = System.currentTimeMillis();
        RScoredSortedSet<String> zset = redissonClient.getScoredSortedSet("order:delay:queue");
        Collection<String> expiredOrderIds = zset.valueRange(0, true, now, true);

        for (String orderId : expiredOrderIds) {
            orderService.cancelOrderById(orderId); 
            zset.remove(orderId);
        }
    }
}
