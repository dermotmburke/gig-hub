package com.d3bot.events.routes;

import com.d3bot.events.deduplicators.EventDeduplicator;
import com.d3bot.events.extractors.EventExtractor;
import com.d3bot.events.fetchers.EventFetcher;
import com.d3bot.events.models.Event;
import com.d3bot.events.notifiers.EventNotifier;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.impl.DefaultCamelContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EventRouteBuilderTest {

    private final EventNotifier notifier = mock(EventNotifier.class);
    private final EventDeduplicator dedup = mock(EventDeduplicator.class);
    private CamelContext context;

    private static final Event EVENT_A = new Event("Artist A", "Venue A", LocalDateTime.of(2026, 5, 1, 19, 0), "/a");
    private static final Event EVENT_B = new Event("Artist B", "Venue B", LocalDateTime.of(2026, 5, 2, 20, 0), "/b");

    static class TestEventRouteBuilder extends EventRouteBuilder {
        TestEventRouteBuilder(EventFetcher fetcher, EventExtractor extractor,
                              List<EventNotifier> notifiers, Optional<EventDeduplicator> dedup) {
            super(fetcher, extractor, notifiers, dedup);
        }
    }

    private void runRoute(List<Event> events, Optional<EventDeduplicator> deduplicator) throws Exception {
        EventFetcher fetcher = () -> "raw";
        EventExtractor extractor = raw -> events;
        TestEventRouteBuilder route = new TestEventRouteBuilder(fetcher, extractor, List.of(notifier), deduplicator);

        context = new DefaultCamelContext();
        context.addRoutes(route);
        context.start();

        try (ProducerTemplate template = context.createProducerTemplate()) {
            template.sendBody("direct:" + route.getRouteId(), null);
        }
    }

    @AfterEach
    void tearDown() throws Exception {
        if (context != null) {
            context.stop();
        }
    }

    @Test
    void runNotifiesWithFetchedEvents() throws Exception {
        runRoute(List.of(EVENT_A, EVENT_B), Optional.empty());
        verify(notifier).notify(List.of(EVENT_A, EVENT_B));
    }

    @Test
    void runFiltersAlreadySentEvents() throws Exception {
        List<Event> all = List.of(EVENT_A, EVENT_B);
        when(dedup.filter(all)).thenReturn(List.of(EVENT_A));

        runRoute(all, Optional.of(dedup));

        verify(notifier).notify(List.of(EVENT_A));
    }

    @Test
    void runMarksNewEventsAsSentAfterNotification() throws Exception {
        List<Event> events = List.of(EVENT_A);
        when(dedup.filter(events)).thenReturn(events);

        runRoute(events, Optional.of(dedup));

        verify(dedup).markSent(events);
    }

    @Test
    void runDoesNotMarkSentWhenNotificationFails() throws Exception {
        List<Event> events = List.of(EVENT_A);
        when(dedup.filter(events)).thenReturn(events);
        doThrow(new RuntimeException("notify failed")).when(notifier).notify(any());

        runRoute(events, Optional.of(dedup));

        verify(dedup, never()).markSent(any());
    }

    @Test
    void runHandlesIOExceptionFromFetchGracefully() throws Exception {
        EventFetcher failingFetcher = () -> { throw new IOException("network error"); };
        TestEventRouteBuilder route = new TestEventRouteBuilder(failingFetcher, raw -> List.of(), List.of(notifier), Optional.empty());

        context = new DefaultCamelContext();
        context.addRoutes(route);
        context.start();

        try (ProducerTemplate template = context.createProducerTemplate()) {
            assertDoesNotThrow(() -> template.sendBody("direct:" + route.getRouteId(), null));
        }
        verifyNoInteractions(notifier);
    }

    @Test
    void runHandlesInterruptedExceptionAndRestoresInterruptFlag() throws Exception {
        EventFetcher interruptedFetcher = () -> { throw new InterruptedException(); };
        TestEventRouteBuilder route = new TestEventRouteBuilder(interruptedFetcher, raw -> List.of(), List.of(notifier), Optional.empty());

        context = new DefaultCamelContext();
        context.addRoutes(route);
        context.start();

        try (ProducerTemplate template = context.createProducerTemplate()) {
            template.sendBody("direct:" + route.getRouteId(), null);
        }

        assertTrue(Thread.currentThread().isInterrupted());
        Thread.interrupted(); // clear for test cleanliness
    }

    @Test
    void routeIdIsDerivedFromClassName() {
        assertEquals("test-pipeline", new TestEventRouteBuilder(() -> "", raw -> List.of(), List.of(), Optional.empty()).getRouteId());
        assertEquals("banquet-pipeline", EventRouteBuilder.deriveRouteId(BanquetEventRouteBuilder.class));
        assertEquals("royal-albert-hall-pipeline", EventRouteBuilder.deriveRouteId(RoyalAlbertHallEventRouteBuilder.class));
    }
}
