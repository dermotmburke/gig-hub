package com.d3bot.events.deduplication;

import com.d3bot.events.models.Event;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.params.SetParams;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EventDeduplicationServiceTest {

    private final JedisPooled jedis = mock(JedisPooled.class);
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private final EventDeduplicationService service = new EventDeduplicationService(jedis, objectMapper);

    private final Event eventA = new Event("Artist A", "Venue A", LocalDateTime.of(2026, 4, 7, 19, 0), "/a");
    private final Event eventB = new Event("Artist B", "Venue B", LocalDateTime.of(2026, 4, 8, 20, 0), "/b");

    @Test
    void filterReturnsEventWhenNotInRedis() {
        when(jedis.mget(eventA.key())).thenReturn(Arrays.asList((String) null));

        assertEquals(List.of(eventA), service.filter(List.of(eventA)));
    }

    @Test
    void filterExcludesEventAlreadyInRedis() {
        when(jedis.mget(eventA.key())).thenReturn(List.of("some-json"));

        assertEquals(List.of(), service.filter(List.of(eventA)));
    }

    @Test
    void filterReturnsMixedResultsInOneRoundTrip() {
        when(jedis.mget(eventA.key(), eventB.key())).thenReturn(Arrays.asList(null, "some-json"));

        assertEquals(List.of(eventA), service.filter(List.of(eventA, eventB)));
    }

    @Test
    void markSentStoresJsonWithTtlViaPipeline() {
        Pipeline pipeline = mock(Pipeline.class);
        when(jedis.pipelined()).thenReturn(pipeline);

        service.markSent(List.of(eventA, eventB));

        verify(pipeline).set(eq(eventA.key()), anyString(), any(SetParams.class));
        verify(pipeline).set(eq(eventB.key()), anyString(), any(SetParams.class));
    }
}
