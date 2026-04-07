package com.d3bot.events.jobs;

import com.d3bot.events.deduplication.EventDeduplicationService;
import com.d3bot.events.models.Event;
import com.d3bot.events.notifiers.EventNotifier;
import com.d3bot.events.scrapers.EventScraper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class EventScrapeJob implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(EventScrapeJob.class);

    private final List<EventScraper> scrapers;
    private final List<EventNotifier> notifiers;
    private final Optional<EventDeduplicationService> deduplication;

    public EventScrapeJob(List<EventScraper> scrapers, List<EventNotifier> notifiers,
                          Optional<EventDeduplicationService> deduplication) {
        this.scrapers = scrapers;
        this.notifiers = notifiers;
        this.deduplication = deduplication;
    }

    @Override
    public void run(String... args) {
        scrape();
    }

    public void scrape() {
        for (EventScraper scraper : scrapers) {
            try {
                List<Event> events = scraper.scrape();
                log.info("{} found {} events", scraper.getClass().getSimpleName(), events.size());

                List<Event> newEvents = deduplication.map(d -> d.filter(events)).orElse(events);
                if (newEvents.size() < events.size()) {
                    log.info("Deduplicated: {} new, {} already sent", newEvents.size(), events.size() - newEvents.size());
                }

                notifiers.forEach(n -> n.notify(newEvents));
                deduplication.ifPresent(d -> d.markSent(newEvents));
            } catch (Exception e) {
                log.error("Failed to scrape {}: {}", scraper.getClass().getSimpleName(), e.getMessage());
            }
        }
    }
}
