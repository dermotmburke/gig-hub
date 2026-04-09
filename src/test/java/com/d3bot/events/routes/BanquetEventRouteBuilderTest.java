package com.d3bot.events.routes;

import com.d3bot.events.extractors.BanquetEventExtractor;
import com.d3bot.events.fetchers.BanquetEventFetcher;
import com.d3bot.events.models.Event;
import com.d3bot.events.notifiers.EventNotifier;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.impl.DefaultCamelContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.io.ClassPathResource;

import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class BanquetEventRouteBuilderTest {

    private final BanquetEventFetcher fetcher = mock(BanquetEventFetcher.class);
    private final EventNotifier notifier = mock(EventNotifier.class);
    private CamelContext context;

    @AfterEach
    void tearDown() throws Exception {
        if (context != null) {
            context.stop();
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void runNotifiesWithCorrectlyParsedEvents() throws Exception {
        String html = Files.readString(new ClassPathResource("events.html").getFile().toPath());
        when(fetcher.fetch()).thenReturn(html);

        BanquetEventRouteBuilder route = new BanquetEventRouteBuilder(
                fetcher, new BanquetEventExtractor(), List.of(notifier), Optional.empty());

        context = new DefaultCamelContext();
        context.addRoutes(route);
        context.start();

        try (ProducerTemplate template = context.createProducerTemplate()) {
            template.sendBody("direct:" + route.getRouteId(), null);
        }

        ArgumentCaptor<List<Event>> captor = ArgumentCaptor.forClass(List.class);
        verify(notifier).notify(captor.capture());
        List<Event> events = captor.getValue();

        assertEquals(47, events.size());
        assertEquals("Lightyear / Slow Gherkin", events.get(0).artist());
        assertEquals(LocalDateTime.of(Year.now().getValue(), 4, 6, 19, 0), events.get(0).dateTime());
        assertEquals("The Fighting Cocks", events.get(0).location());
        assertTrue(events.get(0).url().startsWith("https://"));
    }
}
