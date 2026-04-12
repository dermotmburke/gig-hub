package com.d3bot.events.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import redis.clients.jedis.JedisPooled;

@Component
@ConditionalOnProperty("redis.url")
public class RedisHealthIndicator implements HealthIndicator {

    private final JedisPooled jedis;

    public RedisHealthIndicator(JedisPooled jedis) {
        this.jedis = jedis;
    }

    @Override
    public Health health() {
        try {
            String response = jedis.ping();
            if ("PONG".equals(response)) {
                return Health.up().build();
            }
            return Health.down().withDetail("response", response).build();
        } catch (Exception e) {
            return Health.down(e).build();
        }
    }
}
