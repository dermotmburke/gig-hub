package com.d3bot.events.pipelines;

import com.d3bot.events.deduplicators.EventDeduplicationService;
import com.d3bot.events.extractors.EventExtractor;
import com.d3bot.events.fetchers.EventFetcher;
import com.d3bot.events.models.Event;
import com.d3bot.events.notifiers.EventNotifier;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class EventPipelineTest {

    private final EventNotifier notifier = mock(EventNotifier.class);
    private final EventDeduplicationService dedup = mock(EventDeduplicationService.class);

    private static final Event EVENT_A = new Event("Artist A", "Venue A", LocalDateTime.of(2026, 5, 1, 19, 0), "/a");
    private static final Event EVENT_B = new Event("Artist B", "Venue B", LocalDateTime.of(2026, 5, 2, 20, 0), "/b");

    private EventPipeline pipeline(List<Event> events, Optional<EventDeduplicationService> deduplication) {
        EventFetcher fetcher = () -> "raw";
        EventExtractor extractor = raw -> events;
        return new EventPipeline(fetcher, extractor, List.of(notifier), deduplication) {};
    }

    @Test
    void runNotifiesWithFetchedEvents() {
        pipeline(List.of(EVENT_A, EVENT_B), Optional.empty()).run();
        verify(notifier).notify(List.of(EVENT_A, EVENT_B));
    }

    @Test
    void runFiltersAlreadySentEvents() {
        List<Event> all = List.of(EVENT_A, EVENT_B);
        when(dedup.filter(all)).thenReturn(List.of(EVENT_A));

        pipeline(all, Optional.of(dedup)).run();

        verify(notifier).notify(List.of(EVENT_A));
    }

    @Test
    void runMarksNewEventsAsSentAfterNotification() {
        List<Event> events = List.of(EVENT_A);
        when(dedup.filter(events)).thenReturn(events);

        pipeline(events, Optional.of(dedup)).run();

        verify(dedup).markSent(events);
    }

    @Test
    void runDoesNotMarkSentWhenNotificationFails() {
        List<Event> events = List.of(EVENT_A);
        when(dedup.filter(events)).thenReturn(events);
        doThrow(new RuntimeException("notify failed")).when(notifier).notify(any());

        pipeline(events, Optional.of(dedup)).run();

        verify(dedup, never()).markSent(any());
    }

    @Test
    void runHandlesIOExceptionFromFetchGracefully() {
        EventFetcher failingFetcher = () -> { throw new IOException("network error"); };
        EventPipeline pipeline = new EventPipeline(failingFetcher, raw -> List.of(), List.of(notifier), Optional.empty()) {};

        assertDoesNotThrow(pipeline::run);
        verifyNoInteractions(notifier);
    }

    @Test
    void runHandlesInterruptedExceptionAndRestoresInterruptFlag() {
        EventFetcher interruptedFetcher = () -> { throw new InterruptedException(); };
        EventPipeline pipeline = new EventPipeline(interruptedFetcher, raw -> List.of(), List.of(notifier), Optional.empty()) {};

        pipeline.run();

        assertTrue(Thread.currentThread().isInterrupted());
        Thread.interrupted(); // clear for test cleanliness
    }
}
