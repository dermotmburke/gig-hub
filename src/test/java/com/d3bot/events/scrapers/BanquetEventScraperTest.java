package com.d3bot.events.scrapers;

import com.d3bot.events.extractors.BanquetEventExtractor;
import com.d3bot.events.fetchers.EventFetcher;
import com.d3bot.events.models.Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BanquetEventScraperTest {

    private final EventFetcher eventFetcher = mock(EventFetcher.class);
    private final BanquetEventScraper scraper = new BanquetEventScraper(eventFetcher, new BanquetEventExtractor());

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
        assertEquals(LocalDate.of(Year.now().getValue(), 4, 6), events.get(0).date());
        assertEquals("The Fighting Cocks", events.get(0).location());
        assertTrue(events.get(0).url().startsWith("https://"));
    }
}
