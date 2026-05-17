package com.d3bot.events.pipelines;

import com.d3bot.events.deduplicators.EventDeduplicator;
import com.d3bot.events.extractors.TicketmasterEventExtractor;
import com.d3bot.events.fetchers.TicketmasterEventFetcher;
import com.d3bot.events.notifiers.EventNotifier;

import java.util.List;
import java.util.Optional;

public class TicketmasterVenueEventPipeline extends EventPipeline {

    public TicketmasterVenueEventPipeline(
            String venueName,
            TicketmasterEventFetcher fetcher,
            TicketmasterEventExtractor extractor,
            List<EventNotifier> notifiers,
            Optional<EventDeduplicator> deduplicator) {
        super(venueName + "-pipeline", fetcher, extractor, notifiers, deduplicator);
    }
}
