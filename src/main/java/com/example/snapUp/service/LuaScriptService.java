package com.example.snapUp.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class LuaScriptService {

    @Autowired
    private RedisTemplate redisTemplate;

    public int exeLua(List<String> keys, List<String> ARGV, String scriptPath) throws IOException {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        ClassPathResource luaFile = new ClassPathResource(scriptPath);
        String luaScript = new String(luaFile.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        script.setScriptText(luaScript);
        script.setResultType(Long.class);
        int res = -2;
        try {
            res = Math.toIntExact((Long) redisTemplate.execute(
                    script,
                    keys,
                    ARGV.toArray()));
        } catch (Exception e) {
            Throwable cause = e.getCause();
            System.out.println("Lua 發生錯誤: " + cause.getMessage());
            cause.printStackTrace();
        }
        return res;
    }
}
