package com.d3bot.events;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

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
    EventNotifier eventNotifier;

    @Autowired
    EventScrapeJob eventScrapeJob;

    @Test
    void scrapeNotifiesWithEventsFromEachScraper() throws Exception {
        List<Event> banquetEvents = List.of(new Event("Artist A", "Venue A", "Monday", "/a"));
        List<Event> rahEvents = List.of(new Event("Artist B", "Royal Albert Hall", "Tuesday", "/b"));
        when(banquetEventScraper.scrape()).thenReturn(banquetEvents);
        when(royalAlbertHallEventScraper.scrape()).thenReturn(rahEvents);

        eventScrapeJob.scrape();

        verify(eventNotifier).notify(banquetEvents);
        verify(eventNotifier).notify(rahEvents);
    }
}
