package com.d3bot.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
public class EventScrapeJob {

    private static final Logger log = LoggerFactory.getLogger(EventScrapeJob.class);

    private final EventFetcher eventFetcher;
    private final EventExtractor eventExtractor;
    private final EventNotifier eventNotifier;

    @Value("${scraper.url:https://www.banquetrecords.com/events?w=1000}")
    private String eventsUrl;

    public EventScrapeJob(EventFetcher eventFetcher, EventExtractor eventExtractor, EventNotifier eventNotifier) {
        this.eventFetcher = eventFetcher;
        this.eventExtractor = eventExtractor;
        this.eventNotifier = eventNotifier;
    }

    @Scheduled(fixedRateString = "${scraper.interval-ms:3600000}")
    public void scrape() throws IOException {
        log.info("Scraping events from {}", eventsUrl);
        String html = eventFetcher.fetch(eventsUrl);
        List<Event> events = eventExtractor.extract(html);
        log.info("Found {} events", events.size());
        eventNotifier.notify(events);
    }
}
