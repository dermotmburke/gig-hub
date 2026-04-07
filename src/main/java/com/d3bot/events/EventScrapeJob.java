package com.d3bot.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Component
public class EventScrapeJob {

    private static final Logger log = LoggerFactory.getLogger(EventScrapeJob.class);

    private final EventExtractor eventExtractor;
    private final RestTemplate restTemplate;

    @Value("${scraper.url:https://www.banquetrecords.com/events?w=1000}")
    private String eventsUrl;

    public EventScrapeJob(EventExtractor eventExtractor, RestTemplate restTemplate) {
        this.eventExtractor = eventExtractor;
        this.restTemplate = restTemplate;
    }

    @Scheduled(fixedRateString = "${scraper.interval-ms:3600000}")
    public void scrape() {
        log.info("Scraping events from {}", eventsUrl);
        String html = restTemplate.getForObject(eventsUrl, String.class);
        List<Event> events = eventExtractor.extract(html);
        log.info("Found {} events", events.size());
        // TODO: notify people
        events.forEach(e -> log.info("Event: {}", e));
    }
}
