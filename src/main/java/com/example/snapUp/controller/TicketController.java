package com.example.snapUp.controller;

import com.example.snapUp.dto.PurchaseRequest;
import com.example.snapUp.entity.Ticket;
import com.example.snapUp.repository.TicketRepository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {
    private static final Logger logger = LoggerFactory.getLogger(TicketController.class);

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static String luaPath = "lua/decr_ticket_stock.lua";

    @PostMapping("/reset")
    @Transactional
    public String resetTickets() {
        try {
            logger.info("Attempting to reset tickets");
            Optional<Ticket> optionalTicket = ticketRepository.findById(1L);

            Ticket ticket = optionalTicket.get();
            ticket.setType("A");
            ticket.setStock(20);
            ticketRepository.save(ticket);
            logger.info("Tickets reset successfully");
            return "票券已重設！";
        } catch (Exception e) {
            logger.error("Error resetting tickets", e);
            return "重設票券時發生錯誤：" + e.getMessage();
        }
    }

    @PostMapping("/showRemain")
    public String showRemain() {
        try {
            logger.info("Attempting to show remaining tickets");
            Optional<Ticket> optionalTicket = ticketRepository.findById(1L);
            if (optionalTicket.isPresent()) {
                Ticket ticket = optionalTicket.get();
                logger.info("Found ticket with stock: {}", ticket.getStock());
                return String.valueOf(ticket.getStock());
            }
            logger.warn("No ticket found with ID 1");
            return "找不到票券！";
        } catch (Exception e) {
            logger.error("Error showing remaining tickets", e);
            return "查詢剩餘票券時發生錯誤：" + e.getMessage();
        }
    }

    @PostMapping("/purchase")
    @Transactional
    public String purchaseTicket(@RequestBody PurchaseRequest request) throws IOException {

        Long ticketId = request.getTicketId();
        int quantity = request.getQuantity() != null ? request.getQuantity() : 1;
        String redisKeyStock = "ticket_stock:" + ticketId;

        // 查詢 DB 中的初始票數
        Optional<Ticket> optionalTicket = ticketRepository.findById(request.getTicketId());
        if (optionalTicket.isEmpty()) {
            return "找不到票券！";
        }
        int initStock = optionalTicket.get().getStock();

        // 執行lua
        List<String> keys = List.of(redisKeyStock);
        List<String> argv = List.of(String.valueOf(quantity), String.valueOf(initStock));
        Long result = exeLua(keys, argv);

        // 根據回傳結果處理邏輯
        if (result == null) {
            return "購票失敗：Redis 無回應";
        } else if (result == -1) {
            return "購票失敗：票券不足";
        } else {
            return "購票成功！剩餘票券：" + result;
        }
    }

    private Long exeLua(List<String> keys, List<String> ARGV) throws IOException {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        ClassPathResource luaFile = new ClassPathResource(luaPath);
        String luaScript = new String(luaFile.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        script.setScriptText(luaScript);
        script.setResultType(Long.class);

        return redisTemplate.execute(
                script,
                keys,
                ARGV.toArray()
        );
    }
}