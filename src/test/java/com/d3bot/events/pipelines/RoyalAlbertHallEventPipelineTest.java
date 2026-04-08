package com.d3bot.events.pipelines;

import com.d3bot.events.extractors.TicketmasterEventExtractor;
import com.d3bot.events.fetchers.RoyalAlbertHallEventFetcher;
import com.d3bot.events.models.Event;
import com.d3bot.events.notifiers.EventNotifier;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class RoyalAlbertHallEventPipelineTest {

    private final RoyalAlbertHallEventFetcher fetcher = mock(RoyalAlbertHallEventFetcher.class);
    private final TicketmasterEventExtractor extractor = mock(TicketmasterEventExtractor.class);
    private final EventNotifier notifier = mock(EventNotifier.class);
    private final RoyalAlbertHallEventPipeline pipeline =
            new RoyalAlbertHallEventPipeline(fetcher, extractor, List.of(notifier), Optional.empty());

    @Test
    void runFetchesFromTicketmasterAndNotifies() throws Exception {
        String json = "{\"_embedded\":{\"events\":[]}}";
        List<Event> events = List.of(
                new Event("Nick Cave", "Royal Albert Hall", LocalDateTime.of(2026, 9, 1, 19, 30), "https://example.com")
        );
        when(fetcher.fetch()).thenReturn(json);
        when(extractor.extract(json)).thenReturn(events);

        pipeline.run();

        verify(notifier).notify(events);
    }

    @Test
    void runHandlesInterruptedExceptionFromFetch() throws Exception {
        when(fetcher.fetch()).thenThrow(new InterruptedException("interrupted"));

        pipeline.run();

        verifyNoInteractions(notifier);
        assertTrue(Thread.currentThread().isInterrupted());
        Thread.interrupted(); // clear for test cleanliness
    }
}
