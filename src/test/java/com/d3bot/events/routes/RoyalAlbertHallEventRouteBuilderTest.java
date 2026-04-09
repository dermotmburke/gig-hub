package com.d3bot.events.routes;

import com.d3bot.events.extractors.TicketmasterEventExtractor;
import com.d3bot.events.fetchers.RoyalAlbertHallEventFetcher;
import com.d3bot.events.models.Event;
import com.d3bot.events.notifiers.EventNotifier;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.impl.DefaultCamelContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class RoyalAlbertHallEventRouteBuilderTest {

    private final RoyalAlbertHallEventFetcher fetcher = mock(RoyalAlbertHallEventFetcher.class);
    private final TicketmasterEventExtractor extractor = mock(TicketmasterEventExtractor.class);
    private final EventNotifier notifier = mock(EventNotifier.class);
    private CamelContext context;

    @AfterEach
    void tearDown() throws Exception {
        if (context != null) {
            context.stop();
        }
    }

    @Test
    void runFetchesFromTicketmasterAndNotifies() throws Exception {
        String json = "{\"_embedded\":{\"events\":[]}}";
        List<Event> events = List.of(
                new Event("Nick Cave", "Royal Albert Hall", LocalDateTime.of(2026, 9, 1, 19, 30), "https://example.com")
        );
        when(fetcher.fetch()).thenReturn(json);
        when(extractor.extract(json)).thenReturn(events);

        RoyalAlbertHallEventRouteBuilder route = new RoyalAlbertHallEventRouteBuilder(
                fetcher, extractor, List.of(notifier), Optional.empty());

        context = new DefaultCamelContext();
        context.addRoutes(route);
        context.start();

        try (ProducerTemplate template = context.createProducerTemplate()) {
            template.sendBody("direct:" + route.getRouteId(), null);
        }

        verify(notifier).notify(events);
    }

    @Test
    void runHandlesInterruptedExceptionFromFetch() throws Exception {
        when(fetcher.fetch()).thenThrow(new InterruptedException("interrupted"));

        RoyalAlbertHallEventRouteBuilder route = new RoyalAlbertHallEventRouteBuilder(
                fetcher, extractor, List.of(notifier), Optional.empty());

        context = new DefaultCamelContext();
        context.addRoutes(route);
        context.start();

        try (ProducerTemplate template = context.createProducerTemplate()) {
            template.sendBody("direct:" + route.getRouteId(), null);
        }

        verifyNoInteractions(notifier);
        assertTrue(Thread.currentThread().isInterrupted());
        Thread.interrupted(); // clear for test cleanliness
    }
}
