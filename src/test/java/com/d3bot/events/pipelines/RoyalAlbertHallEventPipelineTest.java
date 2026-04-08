package com.d3bot.events.pipelines;

import com.d3bot.events.extractors.RoyalAlbertHallExtractor;
import com.d3bot.events.fetchers.TicketmasterEventFetcher;
import com.d3bot.events.models.Event;
import com.d3bot.events.notifiers.EventNotifier;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class RoyalAlbertHallEventPipelineTest {

    private final TicketmasterEventFetcher fetcher = mock(TicketmasterEventFetcher.class);
    private final RoyalAlbertHallExtractor extractor = mock(RoyalAlbertHallExtractor.class);
    private final EventNotifier notifier = mock(EventNotifier.class);
    private final RoyalAlbertHallEventPipeline pipeline =
            new RoyalAlbertHallEventPipeline(fetcher, extractor, "test-api-key", "KovZpZAEdntA",
                    List.of(notifier), Optional.empty());

    @Test
    void runFetchesFromTicketmasterAndNotifies() throws Exception {
        String json = "{\"_embedded\":{\"events\":[]}}";
        List<Event> events = List.of(
                new Event("Nick Cave", "Royal Albert Hall", LocalDateTime.of(2026, 9, 1, 19, 30), "https://example.com")
        );
        when(fetcher.fetch("KovZpZAEdntA", "test-api-key")).thenReturn(json);
        when(extractor.extract(json)).thenReturn(events);

        pipeline.run();

        verify(notifier).notify(events);
    }

    @Test
    void runHandlesInterruptedExceptionFromFetch() throws Exception {
        when(fetcher.fetch(any(), any())).thenThrow(new InterruptedException("interrupted"));

        pipeline.run();

        verifyNoInteractions(notifier);
        assertTrue(Thread.currentThread().isInterrupted());
        Thread.interrupted(); // clear for test cleanliness
    }
}
