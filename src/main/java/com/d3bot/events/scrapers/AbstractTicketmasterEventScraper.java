package com.d3bot.events.scrapers;

import com.d3bot.events.extractors.TicketmasterEventExtractor;
import com.d3bot.events.fetchers.TicketmasterEventFetcher;
import com.d3bot.events.models.Event;

import java.io.IOException;
import java.util.List;

/**
 * Base class for Ticketmaster venue scrapers. Subclasses provide the venue ID;
 * all HTTP fetching and JSON extraction is handled here.
 *
 * <p>To add a new venue, extend this class, inject the shared fetcher/extractor
 * and the Ticketmaster API key, and supply the venue-specific ID:
 *
 * <pre>{@code
 * @Component
 * @ConditionalOnProperty({"ticketmaster.api-key", "ticketmaster.venues.my-venue.id"})
 * public class MyVenueEventScraper extends AbstractTicketmasterEventScraper {
 *
 *     public MyVenueEventScraper(
 *             TicketmasterEventFetcher fetcher,
 *             TicketmasterEventExtractor extractor,
 *             @Value("${ticketmaster.api-key}") String apiKey,
 *             @Value("${ticketmaster.venues.my-venue.id}") String venueId) {
 *         super(fetcher, extractor, apiKey, venueId);
 *     }
 * }
 * }</pre>
 */
public abstract class AbstractTicketmasterEventScraper implements EventScraper {

    private final TicketmasterEventFetcher fetcher;
    private final TicketmasterEventExtractor extractor;
    private final String apiKey;
    private final String venueId;

    protected AbstractTicketmasterEventScraper(
            TicketmasterEventFetcher fetcher,
            TicketmasterEventExtractor extractor,
            String apiKey,
            String venueId) {
        this.fetcher = fetcher;
        this.extractor = extractor;
        this.apiKey = apiKey;
        this.venueId = venueId;
    }

    @Override
    public final List<Event> scrape() throws IOException {
        try {
            String json = fetcher.fetch(venueId, apiKey);
            return extractor.extract(json);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Ticketmaster fetch interrupted", e);
        }
    }
}
