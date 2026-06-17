package com.atguigu.yygh.orders.service.impl;

import com.atguigu.yygh.orders.service.RedisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@Slf4j
public class RedisServiceImpl implements RedisService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    // Lua脚本：扣减库存
    // 逻辑：如果 key 存在，并且值 > 0，则执行 DECR 并返回 1；否则返回 0
    private static final String DECREMENT_LUA_SCRIPT = 
            "if redis.call('exists', KEYS[1]) == 1 then " +
            "    local stock = tonumber(redis.call('get', KEYS[1])) " +
            "    if stock > 0 then " +
            "        redis.call('decr', KEYS[1]) " +
            "        return 1 " +
            "    end " +
            "end " +
            "return 0 ";

    // Lua脚本：回滚库存
    // 逻辑：如果 key 存在，则执行 INCR 并返回增加后的值；如果不存在，说明可能过期了，不做处理返回 0
    private static final String INCREMENT_LUA_SCRIPT = 
            "if redis.call('exists', KEYS[1]) == 1 then " +
            "    return redis.call('incr', KEYS[1]) " +
            "else " +
            "    return 0 " +
            "end ";

    @Override
    public boolean decrementStock(String key) {
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(DECREMENT_LUA_SCRIPT);
        redisScript.setResultType(Long.class);

        Long result = stringRedisTemplate.execute(redisScript, Collections.singletonList(key));
        
        boolean success = result != null && result == 1L;
        if (success) {
            log.info("Redis 预扣减库存成功, key: {}", key);
        } else {
            log.warn("Redis 预扣减库存失败(号源已满或未初始化), key: {}", key);
        }
        return success;
    }

    @Override
    public Long incrementStock(String key) {
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(INCREMENT_LUA_SCRIPT);
        redisScript.setResultType(Long.class);

        Long result = stringRedisTemplate.execute(redisScript, Collections.singletonList(key));
        log.info("Redis 回滚库存完毕, key: {}, 当前剩余: {}", key, result);
        return result;
    }

    @Override
    public void initStock(String key, int stock) {
        stringRedisTemplate.opsForValue().set(key, String.valueOf(stock));
        log.info("Redis 初始化号源库存完成, key: {}, stock: {}", key, stock);
    }
}
