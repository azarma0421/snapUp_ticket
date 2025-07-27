package com.example.snapUp.common.services;

import com.example.snapUp.common.exceptions.LockNotAcquireException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.Callable;

@Service
public class LockService {
    private static final Logger logger = LoggerFactory.getLogger(LockService.class);

    private static String lockLuaPath = "lua/lock.lua";

    private static String unlockLuaPath = "lua/unlock.lua";

    @Autowired
    private RedisTemplate redisTemplate;

    public LockService(RedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * executeWithLock
     * @param lockKey Lock Key
     * @param maxRetries Max Retries
     * @param retryDelayMillis Retry Delay Millis
     * @param action What do you want after lock
     * @return
     * @param <T>
     * @throws IOException
     */
    public <T> T executeWithLock(String lockKey, int maxRetries, long retryDelayMillis, Callable<T> action) throws IOException {
        String lockValue = UUID.randomUUID().toString();

        boolean isLocked = Lock(lockKey, lockValue, maxRetries, retryDelayMillis);
        if (isLocked) {
            try {
                return action.call();
            } catch (IOException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                unlock(lockKey, lockValue);
            }
        } else {
            throw new LockNotAcquireException("Cant acquire lock, lockKey: " + lockKey);
        }
    }

    /**
     * redis Lock
     */
    public boolean Lock(String lockKey, String lockValue, int maxRetries, long retryDelayMillis) throws IOException {
        boolean isLocked = false;
        // 重試取鎖
        for (int retryTimes = 1; retryTimes <= maxRetries; retryTimes++) {
            isLocked = locking(lockKey, lockValue, 5000);
            if (isLocked) {
                break;
            }
            if (retryTimes < maxRetries - 1) {
                try {
                    Thread.sleep(retryDelayMillis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        return isLocked;
    }

    private boolean locking(String lockKey, String lockValue, long exp) throws IOException {
        logger.info("lockKey: {}, lockValue: {}, exp: {}", lockKey, lockValue, exp);
        DefaultRedisScript<String> script = new DefaultRedisScript<>();
        ClassPathResource luaFile = new ClassPathResource(lockLuaPath);
        String luaScript = new String(luaFile.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        script.setScriptText(luaScript);
        script.setResultType(String.class);

        String result = (String) redisTemplate.execute(
                script,
                Collections.singletonList(lockKey),
                lockValue,
                String.valueOf(exp));

        // TODO watckdog

        return "OK".equals(result);
    }

    public boolean unlock(String lockKey, String lockValue) throws IOException {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        ClassPathResource luaFile = new ClassPathResource(unlockLuaPath);
        String luaScript = new String(luaFile.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        script.setScriptText(luaScript);
        script.setResultType(Long.class);

        Long result = (Long) redisTemplate.execute(
                script,
                Collections.singletonList(lockKey),
                lockValue);

        return result == 1L;
    }
}
