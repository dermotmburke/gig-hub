package com.d3bot.events.config;

import com.d3bot.events.deduplicators.EventDeduplicator;
import com.d3bot.events.extractors.TicketmasterEventExtractor;
import com.d3bot.events.notifiers.EventNotifier;
import com.d3bot.events.pipelines.TicketmasterVenueEventPipeline;
import com.d3bot.events.utilities.UrlFetcher;
import com.d3bot.events.fetchers.TicketmasterEventFetcher;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Factory that creates {@link TicketmasterVenueEventPipeline} instances for dynamically
 * configured venues. Holds all shared dependencies so that bean definitions registered by
 * {@link TicketmasterVenueBeanRegistrar} only need to supply venue-specific arguments.
 */
@Component
public class TicketmasterPipelineFactory {

    private final UrlFetcher urlFetcher;
    private final TicketmasterEventExtractor extractor;
    private final List<EventNotifier> notifiers;
    private final Optional<EventDeduplicator> deduplicator;

    public TicketmasterPipelineFactory(
            UrlFetcher urlFetcher,
            TicketmasterEventExtractor extractor,
            List<EventNotifier> notifiers,
            Optional<EventDeduplicator> deduplicator) {
        this.urlFetcher = urlFetcher;
        this.extractor = extractor;
        this.notifiers = notifiers;
        this.deduplicator = deduplicator;
    }

    public TicketmasterVenueEventPipeline create(String venueName, String venueId, String apiKey) {
        TicketmasterEventFetcher fetcher = new TicketmasterEventFetcher(urlFetcher, venueId, apiKey);
        return new TicketmasterVenueEventPipeline(venueName, fetcher, extractor, notifiers, deduplicator);
    }
}
