package com.d3bot.events.health;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import redis.clients.jedis.JedisPooled;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RedisHealthIndicatorTest {

    private final JedisPooled jedis = mock(JedisPooled.class);
    private final RedisHealthIndicator indicator = new RedisHealthIndicator(jedis);

    @Test
    void returnsUpWhenRedisPingsSuccessfully() {
        when(jedis.ping()).thenReturn("PONG");

        Health health = indicator.health();

        assertEquals(Status.UP, health.getStatus());
    }

    @Test
    void returnsDownWhenResponseIsNotPong() {
        when(jedis.ping()).thenReturn("unexpected");

        Health health = indicator.health();

        assertEquals(Status.DOWN, health.getStatus());
        assertEquals("unexpected", health.getDetails().get("response"));
    }

    @Test
    void returnsDownWhenExceptionIsThrown() {
        when(jedis.ping()).thenThrow(new RuntimeException("connection refused"));

        Health health = indicator.health();

        assertEquals(Status.DOWN, health.getStatus());
    }
}
