package com.atguigu.yygh.appointment.infrastructure.redis;

import com.atguigu.yygh.appointment.domain.token.TokenGateService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class RedisTokenGateService implements TokenGateService {

    private static final String DECREMENT_SCRIPT =
            "if redis.call('exists', KEYS[1]) == 1 then " +
            " local stock = tonumber(redis.call('get', KEYS[1])) " +
            " if stock > 0 then " +
            "   redis.call('decr', KEYS[1]) " +
            "   return 1 " +
            " end " +
            "end " +
            "return 0";

    private static final String INCREMENT_SCRIPT =
            "if redis.call('exists', KEYS[1]) == 1 then " +
            " return redis.call('incr', KEYS[1]) " +
            "else " +
            " return 0 " +
            "end";

    private final StringRedisTemplate stringRedisTemplate;

    public RedisTokenGateService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public void initScheduleToken(String scheduleId, int stock) {
        stringRedisTemplate.opsForValue().set(buildKey(scheduleId), String.valueOf(stock));
    }

    @Override
    public boolean tryAcquireScheduleToken(String scheduleId) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(DECREMENT_SCRIPT);
        script.setResultType(Long.class);
        Long result = stringRedisTemplate.execute(script, Collections.singletonList(buildKey(scheduleId)));
        return result != null && result == 1L;
    }

    @Override
    public Long releaseScheduleToken(String scheduleId) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(INCREMENT_SCRIPT);
        script.setResultType(Long.class);
        return stringRedisTemplate.execute(script, Collections.singletonList(buildKey(scheduleId)));
    }

    private String buildKey(String scheduleId) {
        return "AP:SCHEDULE:TOKEN:" + scheduleId;
    }
}
