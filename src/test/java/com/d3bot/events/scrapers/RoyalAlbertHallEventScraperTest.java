package com.d3bot.events.scrapers;

import com.d3bot.events.fetchers.EventFetcher;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RoyalAlbertHallEventScraperTest {

    private final EventFetcher eventFetcher = mock(EventFetcher.class);
    private final RoyalAlbertHallEventScraper scraper = new RoyalAlbertHallEventScraper(eventFetcher);

    @Test
    void scrapeReturnsEmptyList() throws Exception {
        ReflectionTestUtils.setField(scraper, "url", "https://test.example.com");
        when(eventFetcher.fetch(any())).thenReturn("<html></html>");

        assertTrue(scraper.scrape().isEmpty());
    }
}
