package com.example.snapUp.common.exceptions;

public class LockNotAcquireException extends RuntimeException {
    public LockNotAcquireException(String msg) {
        super(msg);
    }
}
