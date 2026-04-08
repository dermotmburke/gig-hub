package com.d3bot.events.pipelines;

import com.d3bot.events.deduplicators.EventDeduplicationService;
import com.d3bot.events.extractors.TicketmasterEventExtractor;
import com.d3bot.events.fetchers.RoyalAlbertHallEventFetcher;
import com.d3bot.events.notifiers.EventNotifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@ConditionalOnProperty({"fetchers.ticketmaster.api-key", "fetchers.ticketmaster.venues.royalalberthall.id"})
public class RoyalAlbertHallEventPipeline extends EventPipeline {

    public RoyalAlbertHallEventPipeline(
            RoyalAlbertHallEventFetcher fetcher,
            TicketmasterEventExtractor extractor,
            List<EventNotifier> notifiers,
            Optional<EventDeduplicationService> deduplication) {
        super(fetcher, extractor, notifiers, deduplication);
    }
}
