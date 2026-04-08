package com.d3bot.events.scrapers;

import com.d3bot.events.extractors.TicketmasterEventExtractor;
import com.d3bot.events.fetchers.TicketmasterEventFetcher;
import com.d3bot.events.models.Event;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
@ConditionalOnProperty({"ticketmaster.api-key", "ticketmaster.venue-id"})
public class TicketmasterEventScraper implements EventScraper {

    private final TicketmasterEventFetcher fetcher;
    private final TicketmasterEventExtractor extractor;
    private final String venueId;
    private final String apiKey;

    public TicketmasterEventScraper(
            TicketmasterEventFetcher fetcher,
            TicketmasterEventExtractor extractor,
            @Value("${ticketmaster.venue-id}") String venueId,
            @Value("${ticketmaster.api-key}") String apiKey) {
        this.fetcher = fetcher;
        this.extractor = extractor;
        this.venueId = venueId;
        this.apiKey = apiKey;
    }

    @Override
    public List<Event> scrape() throws IOException {
        try {
            String json = fetcher.fetch(venueId, apiKey);
            return extractor.extract(json);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Ticketmaster fetch interrupted", e);
        }
    }
}
