package com.d3bot.events.deduplication;

import com.d3bot.events.models.Event;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.params.SetParams;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@ConditionalOnProperty("redis.url")
public class EventDeduplicationService {

    private static final Logger log = LoggerFactory.getLogger(EventDeduplicationService.class);

    private final JedisPooled jedis;
    private final ObjectMapper objectMapper;

    public EventDeduplicationService(JedisPooled jedis, ObjectMapper objectMapper) {
        this.jedis = jedis;
        this.objectMapper = objectMapper;
    }

    public List<Event> filter(List<Event> events) {
        if (events.isEmpty()) {
            return events;
        }
        String[] keys = events.stream().map(Event::key).toArray(String[]::new);
        List<String> values = jedis.mget(keys);
        List<Event> newEvents = new ArrayList<>();
        for (int i = 0; i < events.size(); i++) {
            if (values.get(i) == null) {
                newEvents.add(events.get(i));
            }
        }
        return newEvents;
    }

    private static long ttlSecondsFor(Event event) {
        LocalDateTime expiry = event.dateTime().toLocalDate().plusDays(1).atStartOfDay();
        long seconds = Duration.between(LocalDateTime.now(), expiry).getSeconds();
        return Math.max(seconds, 60L);
    }

    public void markSent(List<Event> events) {
        if (events.isEmpty()) {
            return;
        }
        try (Pipeline pipeline = jedis.pipelined()) {
            for (Event event : events) {
                String json = objectMapper.writeValueAsString(event);
                pipeline.set(event.key(), json, SetParams.setParams().ex(ttlSecondsFor(event)));
            }
        } catch (Exception ex) {
            log.error("Failed to store events in Redis", ex);
        }
    }
}
