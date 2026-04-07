package com.d3bot.events.jobs;

import com.d3bot.events.models.Event;
import com.d3bot.events.notifiers.EventNotifier;
import com.d3bot.events.scrapers.EventScraper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EventScrapeJob {

    private static final Logger log = LoggerFactory.getLogger(EventScrapeJob.class);

    private final List<EventScraper> scrapers;
    private final List<EventNotifier> notifiers;

    public EventScrapeJob(List<EventScraper> scrapers, List<EventNotifier> notifiers) {
        this.scrapers = scrapers;
        this.notifiers = notifiers;
    }

    @Scheduled(fixedRateString = "${scraper.interval-ms:3600000}", initialDelayString = "${scraper.initial-delay-ms:0}")
    public void scrape() {
        for (EventScraper scraper : scrapers) {
            try {
                List<Event> events = scraper.scrape();
                log.info("{} found {} events", scraper.getClass().getSimpleName(), events.size());
                notifiers.forEach(n -> n.notify(events));
            } catch (Exception e) {
                log.error("Failed to scrape {}: {}", scraper.getClass().getSimpleName(), e.getMessage());
            }
        }
    }
}
