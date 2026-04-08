package com.d3bot.events.scrapers;

import com.d3bot.events.extractors.TicketmasterEventExtractor;
import com.d3bot.events.fetchers.TicketmasterEventFetcher;
import com.d3bot.events.models.Event;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TicketmasterEventScraperTest {

    private final TicketmasterEventFetcher fetcher = mock(TicketmasterEventFetcher.class);
    private final TicketmasterEventExtractor extractor = mock(TicketmasterEventExtractor.class);
    private final TicketmasterEventScraper scraper = new TicketmasterEventScraper(fetcher, extractor, "test-api-key", "KovZpZAEdntA") {};

    @Test
    void scrapeReturnsDelegatedEvents() throws Exception {
        String json = "{\"_embedded\":{\"events\":[]}}";
        List<Event> expected = List.of(
                new Event("The National", "O2 Academy Brixton", LocalDateTime.of(2026, 6, 12, 19, 30), "https://example.com")
        );
        when(fetcher.fetch("KovZpZAEdntA", "test-api-key")).thenReturn(json);
        when(extractor.extract(json)).thenReturn(expected);

        List<Event> result = scraper.scrape();

        assertEquals(expected, result);
        verify(fetcher).fetch("KovZpZAEdntA", "test-api-key");
        verify(extractor).extract(json);
    }

    @Test
    void scrapeWrapsInterruptedExceptionAsIOException() throws Exception {
        when(fetcher.fetch(any(), any())).thenThrow(new InterruptedException("interrupted"));

        assertThrows(IOException.class, () -> scraper.scrape());
    }
}
