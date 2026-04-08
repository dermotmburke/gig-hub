package com.d3bot.events.scrapers;

import com.d3bot.events.extractors.TicketmasterEventExtractor;
import com.d3bot.events.fetchers.TicketmasterEventFetcher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty({"ticketmaster.api-key", "ticketmaster.royalalberthall.venue-id"})
public class RoyalAlbertHallEventScraper extends TicketmasterEventScraper {

    public RoyalAlbertHallEventScraper(
            TicketmasterEventFetcher fetcher,
            TicketmasterEventExtractor extractor,
            @Value("${ticketmaster.api-key}") String apiKey,
            @Value("${ticketmaster.royalalberthall.venue-id}") String venueId) {
        super(fetcher, extractor, apiKey, venueId);
    }
}
