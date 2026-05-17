package com.d3bot.events.pipelines;

import com.d3bot.events.deduplicators.EventDeduplicator;
import com.d3bot.events.extractors.EventExtractor;
import com.d3bot.events.fetchers.EventFetcher;
import com.d3bot.events.models.Event;
import com.d3bot.events.notifiers.EventNotifier;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EventPipelineTest {

    private final EventNotifier notifier = mock(EventNotifier.class);
    private final EventDeduplicator dedup = mock(EventDeduplicator.class);

    private static final Event EVENT_A = new Event("Artist A", "Venue A", LocalDateTime.of(2026, 5, 1, 19, 0), "/a");
    private static final Event EVENT_B = new Event("Artist B", "Venue B", LocalDateTime.of(2026, 5, 2, 20, 0), "/b");

    static class TestEventPipeline extends EventPipeline {
        TestEventPipeline(EventFetcher fetcher, EventExtractor extractor,
                          List<EventNotifier> notifiers, Optional<EventDeduplicator> dedup) {
            super(fetcher, extractor, notifiers, dedup);
        }
    }

    @Test
    void runNotifiesWithFetchedEvents() {
        EventFetcher fetcher = () -> "raw";
        EventExtractor extractor = raw -> List.of(EVENT_A, EVENT_B);
        new TestEventPipeline(fetcher, extractor, List.of(notifier), Optional.empty()).run();
        verify(notifier).notify(List.of(EVENT_A, EVENT_B));
    }

    @Test
    void runFiltersAlreadySentEvents() {
        List<Event> all = List.of(EVENT_A, EVENT_B);
        when(dedup.filter(all)).thenReturn(List.of(EVENT_A));

        new TestEventPipeline(() -> "raw", raw -> all, List.of(notifier), Optional.of(dedup)).run();

        verify(notifier).notify(List.of(EVENT_A));
    }

    @Test
    void runMarksNewEventsAsSentAfterNotification() {
        List<Event> events = List.of(EVENT_A);
        when(dedup.filter(events)).thenReturn(events);

        new TestEventPipeline(() -> "raw", raw -> events, List.of(notifier), Optional.of(dedup)).run();

        verify(dedup).markSent(events);
    }

    @Test
    void runDoesNotMarkSentWhenNotificationFails() {
        List<Event> events = List.of(EVENT_A);
        when(dedup.filter(events)).thenReturn(events);
        doThrow(new RuntimeException("notify failed")).when(notifier).notify(any());

        new TestEventPipeline(() -> "raw", raw -> events, List.of(notifier), Optional.of(dedup)).run();

        verify(dedup, never()).markSent(any());
    }

    @Test
    void runHandlesIOExceptionFromFetchGracefully() {
        EventFetcher failingFetcher = () -> { throw new IOException("network error"); };
        assertDoesNotThrow(() ->
                new TestEventPipeline(failingFetcher, raw -> List.of(), List.of(notifier), Optional.empty()).run());
        verifyNoInteractions(notifier);
    }

    @Test
    void runHandlesInterruptedExceptionAndRestoresInterruptFlag() {
        EventFetcher interruptedFetcher = () -> { throw new InterruptedException(); };
        new TestEventPipeline(interruptedFetcher, raw -> List.of(), List.of(notifier), Optional.empty()).run();
        assertTrue(Thread.currentThread().isInterrupted());
        Thread.interrupted(); // clear for test cleanliness
    }

    @Test
    void pipelineIdIsDerivedFromClassName() {
        assertEquals("test-pipeline",
                new TestEventPipeline(() -> "", raw -> List.of(), List.of(), Optional.empty()).getPipelineId());
    }
}
