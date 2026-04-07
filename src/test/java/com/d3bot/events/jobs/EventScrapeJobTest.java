package com.d3bot.events.jobs;

import com.d3bot.events.deduplication.EventDeduplicationService;
import com.d3bot.events.models.Event;
import com.d3bot.events.notifiers.LoggingEventNotifier;
import com.d3bot.events.scrapers.BanquetEventScraper;
import com.d3bot.events.scrapers.RoyalAlbertHallEventScraper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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
        List<Event> banquetEvents = List.of(new Event("Artist A", "Venue A", LocalDateTime.of(2026, 4, 7, 19, 0), "/a"));
        List<Event> rahEvents = List.of(new Event("Artist B", "Royal Albert Hall", LocalDateTime.of(2026, 4, 8, 19, 0), "/b"));
        when(banquetEventScraper.scrape()).thenReturn(banquetEvents);
        when(royalAlbertHallEventScraper.scrape()).thenReturn(rahEvents);

        eventScrapeJob.scrape();

        verify(loggingEventNotifier).notify(banquetEvents);
        verify(loggingEventNotifier).notify(rahEvents);
    }

    @Test
    void scrapeFiltersAlreadySentEventsWhenDeduplicationPresent() throws Exception {
        var newEvent = new Event("Artist A", "Venue A", LocalDateTime.of(2026, 4, 7, 19, 0), "/a");
        var sentEvent = new Event("Artist B", "Venue B", LocalDateTime.of(2026, 4, 8, 20, 0), "/b");
        List<Event> allEvents = List.of(newEvent, sentEvent);

        EventDeduplicationService dedup = org.mockito.Mockito.mock(EventDeduplicationService.class);
        when(dedup.filter(allEvents)).thenReturn(List.of(newEvent));
        when(banquetEventScraper.scrape()).thenReturn(allEvents);
        when(royalAlbertHallEventScraper.scrape()).thenReturn(List.of());

        EventScrapeJob jobWithDedup = new EventScrapeJob(
                List.of(banquetEventScraper, royalAlbertHallEventScraper),
                List.of(loggingEventNotifier),
                Optional.of(dedup)
        );
        jobWithDedup.scrape();

        verify(loggingEventNotifier).notify(List.of(newEvent));
        verify(dedup).markSent(List.of(newEvent));
    }
}
