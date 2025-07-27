package com.example.snapUp.common.constant;

public class RedisConstants {
    /**
     * Ticket lock key prefix with ticketType
     */
    public static final String TICKET_LOCK_KEY_PREFIX = "lock:ticket:";

    /**
     * Ticket stock key prefix with ticketType
     */
    public static final String TICKET_STOCK_KEY_PREFIX = "ticket_stock:";

    /**
     * Order cancel key prefix with ticketType
     */
    public static final String ORDER_CANCEL_KEY_PREFIX = "cancel_order:";


    public static final String ORDER_DELAY_QUEUE = "order:delay:queue";

    public static final int INIT_REDIS_STOCK_IF_NOT_EXT = 0;
}
