package com.d3bot.events.config;

import com.d3bot.events.deduplicators.EventDeduplicator;
import com.d3bot.events.extractors.TicketmasterEventExtractor;
import com.d3bot.events.fetchers.TicketmasterEventFetcher;
import com.d3bot.events.notifiers.EventNotifier;
import com.d3bot.events.routes.TicketmasterVenueEventRouteBuilder;
import com.d3bot.events.utilities.UrlFetcher;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Factory that creates {@link TicketmasterVenueEventRouteBuilder} instances for dynamically
 * configured venues. Holds all shared dependencies so that bean definitions registered by
 * {@link TicketmasterVenueBeanRegistrar} only need to supply venue-specific arguments.
 */
@Component
public class TicketmasterRouteBuilderFactory {

    private final UrlFetcher urlFetcher;
    private final TicketmasterEventExtractor extractor;
    private final List<EventNotifier> notifiers;
    private final Optional<EventDeduplicator> deduplicator;

    public TicketmasterRouteBuilderFactory(
            UrlFetcher urlFetcher,
            TicketmasterEventExtractor extractor,
            List<EventNotifier> notifiers,
            Optional<EventDeduplicator> deduplicator) {
        this.urlFetcher = urlFetcher;
        this.extractor = extractor;
        this.notifiers = notifiers;
        this.deduplicator = deduplicator;
    }

    public TicketmasterVenueEventRouteBuilder create(String venueName, String venueId, String apiKey) {
        TicketmasterEventFetcher fetcher = new TicketmasterEventFetcher(urlFetcher, venueId, apiKey);
        return new TicketmasterVenueEventRouteBuilder(venueName, fetcher, extractor, notifiers, deduplicator);
    }
}
