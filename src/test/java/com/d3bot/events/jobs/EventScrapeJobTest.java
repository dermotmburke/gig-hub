package com.d3bot.events.jobs;

import com.d3bot.events.models.Event;
import com.d3bot.events.notifiers.LoggingEventNotifier;
import com.d3bot.events.scrapers.BanquetEventScraper;
import com.d3bot.events.scrapers.RoyalAlbertHallEventScraper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = "scraper.initial-delay-ms=999999999")
class EventScrapeJobTest {

    @MockBean
    BanquetEventScraper banquetEventScraper;

    @MockBean
    RoyalAlbertHallEventScraper royalAlbertHallEventScraper;

    @MockBean
    LoggingEventNotifier loggingEventNotifier;

    @Autowired
    EventScrapeJob eventScrapeJob;

    @Test
    void scrapeNotifiesWithEventsFromEachScraper() throws Exception {
        List<Event> banquetEvents = List.of(new Event("Artist A", "Venue A", LocalDate.of(2026, 4, 7), LocalTime.of(19, 0), "/a"));
        List<Event> rahEvents = List.of(new Event("Artist B", "Royal Albert Hall", LocalDate.of(2026, 4, 8), LocalTime.of(19, 0), "/b"));
        when(banquetEventScraper.scrape()).thenReturn(banquetEvents);
        when(royalAlbertHallEventScraper.scrape()).thenReturn(rahEvents);

        eventScrapeJob.scrape();

        verify(loggingEventNotifier).notify(banquetEvents);
        verify(loggingEventNotifier).notify(rahEvents);
    }
}
