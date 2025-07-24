package com.example.snapUp.common.constant;

public class TicketConstants {
    public static final String TICKET_LOCK_KEY_PREFIX = "lock:ticket:";
    public static final String TICKET_STOCK_KEY_PREFIX = "ticket_stock:";

    public static final int MAX_LOCK_RETRIES = 5;
    public static final long LOCK_RETRY_DELAY_MS = 100;

    public static final int INIT_REDIS_STOCK_IF_NOT_EXT = 0;
}
