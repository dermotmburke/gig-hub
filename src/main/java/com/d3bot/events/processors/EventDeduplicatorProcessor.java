package com.d3bot.events.routes;

import com.d3bot.events.deduplicators.EventDeduplicator;
import com.d3bot.events.models.Event;
import jakarta.annotation.Nonnull;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

class EventDeduplicatorProcessor implements Processor {

    private static final Logger log = LoggerFactory.getLogger(EventDeduplicatorProcessor.class);

    private final Optional<EventDeduplicator> deduplicator;

    EventDeduplicatorProcessor(Optional<EventDeduplicator> deduplicator) {
        this.deduplicator = deduplicator;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void process(Exchange exchange) {
        List<Event> events = exchange.getMessage().getBody(List.class);
        String routeId = exchange.getFromRouteId();
        log.info("{} found {} events", routeId, events.size());

        List<Event> newEvents = getNewEvents(events);
        exchange.getMessage().setBody(newEvents);
    }

    @Nonnull
    private List<Event> getNewEvents(List<Event> events) {
        List<Event> newEvents = deduplicator.map(d -> d.filter(events)).orElse(events);
        if (newEvents.size() < events.size()) {
            log.info("Deduplicated: {} new, {} already sent",
                    newEvents.size(), events.size() - newEvents.size());
        }
        return newEvents;
    }
}
