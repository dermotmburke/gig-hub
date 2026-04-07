package com.d3bot.events;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BanquetEventScraperTest {

    private final EventFetcher eventFetcher = mock(EventFetcher.class);
    private final BanquetEventScraper scraper = new BanquetEventScraper(eventFetcher, new EventExtractor());

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(scraper, "url", "https://test.example.com");
    }

    @Test
    void scrapeReturnsCorrectEvents() throws Exception {
        String html = Files.readString(new ClassPathResource("events.html").getFile().toPath());
        when(eventFetcher.fetch(any())).thenReturn(html);

        List<Event> events = scraper.scrape();

        assertEquals(47, events.size());
        assertEquals("Lightyear / Slow Gherkin", events.get(0).artist());
        assertEquals("Monday 6th April", events.get(0).date());
        assertEquals("The Fighting Cocks", events.get(0).Location());
    }
}
