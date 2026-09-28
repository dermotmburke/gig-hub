package com.d3bot.events.deduplicators;

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
public class EventDeduplicator {

    private static final Logger log = LoggerFactory.getLogger(EventDeduplicator.class);
    public static final int TTL_DAYS_AFTER_EVENT = 2; // expire event from cache 2 days after event - just in case it still appears in fetched list
    // floor for events dated in the past (e.g. a mis-parsed year) - must outlive the fetch interval or they get re-sent every run
    static final long MIN_TTL_SECONDS = Duration.ofDays(30).getSeconds();

    private final JedisPooled jedis;
    private final ObjectMapper objectMapper;

    public EventDeduplicator(JedisPooled jedis, ObjectMapper objectMapper) {
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

    static long ttlSecondsFor(Event event) {
        LocalDateTime expiry = event.dateTime().toLocalDate().plusDays(TTL_DAYS_AFTER_EVENT).atStartOfDay();
        long seconds = Duration.between(LocalDateTime.now(), expiry).getSeconds();
        return Math.max(seconds, MIN_TTL_SECONDS);
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
