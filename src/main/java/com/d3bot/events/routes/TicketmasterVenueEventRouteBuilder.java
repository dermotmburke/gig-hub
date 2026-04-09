package com.d3bot.events.routes;

import com.d3bot.events.deduplicators.EventDeduplicator;
import com.d3bot.events.extractors.TicketmasterEventExtractor;
import com.d3bot.events.fetchers.TicketmasterEventFetcher;
import com.d3bot.events.notifiers.EventNotifier;

import java.util.List;
import java.util.Optional;

/**
 * Concrete {@link EventRouteBuilder} for a Ticketmaster venue configured via properties.
 * The route ID is {@code <venueName>-pipeline} (e.g. {@code brixton-academy-pipeline}).
 */
public class TicketmasterVenueEventRouteBuilder extends EventRouteBuilder {

    public TicketmasterVenueEventRouteBuilder(
            String venueName,
            TicketmasterEventFetcher fetcher,
            TicketmasterEventExtractor extractor,
            List<EventNotifier> notifiers,
            Optional<EventDeduplicator> deduplicator) {
        super(venueName + "-pipeline", fetcher, extractor, notifiers, deduplicator);
    }
}
