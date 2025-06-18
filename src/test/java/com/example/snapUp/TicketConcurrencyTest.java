package com.example.snapUp;

import com.example.snapUp.controller.TicketController;
import com.example.snapUp.dto.PurchaseRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TicketConcurrencyTest {

    @Autowired
    private TicketController ticketController;

    @Test
    public void testHighConcurrencyBuying() throws InterruptedException {
        System.out.println(">>> TicketConcurrencyTest 正在執行");
        final int threadCount = 50;
        AtomicInteger successCount = new AtomicInteger();

        ticketController.resetTickets("1");
        PurchaseRequest purchaseRequest = new PurchaseRequest(1L, 1);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            int userId = i + 1;
            new Thread(() -> {
                try {
                    int res = ticketController.purchaseTicket(purchaseRequest);

                    if (res == -3) {
                        System.out.println("用戶 " + userId + " 購票失敗：超時");
                    } else if (res == -2) {
                        System.out.println("Lua 發生錯誤");
                    } else if (res == -1) {
                        System.out.println("用戶 " + userId + " 購票失敗：票券不足");
                    } else {
                        successCount.getAndIncrement();
                        int tickets = Integer.parseInt(ticketController.showRemain());
                        System.out.println("用戶 " + userId + " 購票成功，票券剩餘: " + tickets);
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
}
