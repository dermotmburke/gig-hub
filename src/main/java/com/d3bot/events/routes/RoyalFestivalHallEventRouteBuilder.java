package com.d3bot.events.routes;

import com.d3bot.events.deduplicators.EventDeduplicator;
import com.d3bot.events.extractors.TicketmasterEventExtractor;
import com.d3bot.events.fetchers.RoyalFestivalHallEventFetcher;
import com.d3bot.events.notifiers.EventNotifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@ConditionalOnProperty("fetchers.ticketmaster.api-key")
public class RoyalFestivalHallEventRouteBuilder extends EventRouteBuilder {

    public RoyalFestivalHallEventRouteBuilder(
            RoyalFestivalHallEventFetcher fetcher,
            TicketmasterEventExtractor extractor,
            List<EventNotifier> notifiers,
            Optional<EventDeduplicator> deduplicator) {
        super(fetcher, extractor, notifiers, deduplicator);
    }
}
