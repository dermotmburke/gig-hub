package com.d3bot.events.scrapers;

import com.d3bot.events.extractors.TicketmasterEventExtractor;
import com.d3bot.events.fetchers.TicketmasterEventFetcher;
import com.d3bot.events.models.Event;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RoyalAlbertHallEventScraperTest {

    private final TicketmasterEventFetcher fetcher = mock(TicketmasterEventFetcher.class);
    private final TicketmasterEventExtractor extractor = mock(TicketmasterEventExtractor.class);
    private final RoyalAlbertHallEventScraper scraper =
            new RoyalAlbertHallEventScraper(fetcher, extractor, "test-api-key", "KovZpZAEdntA");

    @Test
    void scrapeReturnsDelegatedEvents() throws Exception {
        String json = "{\"_embedded\":{\"events\":[]}}";
        List<Event> expected = List.of(
                new Event("Nick Cave", "Royal Albert Hall", LocalDateTime.of(2026, 9, 1, 19, 30), "https://example.com")
        );
        when(fetcher.fetch("KovZpZAEdntA", "test-api-key")).thenReturn(json);
        when(extractor.extract(json)).thenReturn(expected);

        assertEquals(expected, scraper.scrape());
    }
}
