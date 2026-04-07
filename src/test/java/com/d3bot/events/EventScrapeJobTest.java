package com.d3bot.events;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ClassPathResource;

import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = "scraper.initial-delay-ms=999999999")
class EventScrapeJobTest {

    @MockBean
    EventFetcher eventFetcher;

    @MockBean
    LoggingEventNotifier loggingEventNotifier;

    @Autowired
    EventScrapeJob eventScrapeJob;

    @Test
    void scrapePassesExtractedEventsToNotifier() throws Exception {
        String html = Files.readString(new ClassPathResource("events.html").getFile().toPath());
        when(eventFetcher.fetch()).thenReturn(html);

        eventScrapeJob.scrape();

        ArgumentCaptor<List<Event>> captor = ArgumentCaptor.captor();
        verify(loggingEventNotifier).notify(captor.capture());
        List<Event> events = captor.getValue();
        assertEquals(47, events.size());
        assertEquals("Lightyear / Slow Gherkin", events.get(0).artist());
        assertEquals("Monday 6th April", events.get(0).date());
        assertEquals("The Fighting Cocks", events.get(0).Location());
    }
}
