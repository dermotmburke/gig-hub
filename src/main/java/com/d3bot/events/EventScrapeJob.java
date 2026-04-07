package com.d3bot.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EventScrapeJob {

    private static final Logger log = LoggerFactory.getLogger(EventScrapeJob.class);

    private final List<EventScraper> scrapers;
    private final EventNotifier eventNotifier;

    public EventScrapeJob(List<EventScraper> scrapers, EventNotifier eventNotifier) {
        this.scrapers = scrapers;
        this.eventNotifier = eventNotifier;
    }

    @Scheduled(fixedRateString = "${scraper.interval-ms:3600000}", initialDelayString = "${scraper.initial-delay-ms:0}")
    public void scrape() {
        for (EventScraper scraper : scrapers) {
            try {
                List<Event> events = scraper.scrape();
                log.info("{} found {} events", scraper.getClass().getSimpleName(), events.size());
                eventNotifier.notify(events);
            } catch (Exception e) {
                log.error("Failed to scrape {}: {}", scraper.getClass().getSimpleName(), e.getMessage());
            }
        }
    }
}
