package com.d3bot.events.routes;

import com.d3bot.events.deduplicators.EventDeduplicationService;
import com.d3bot.events.models.Event;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

class EventDeduplicationProcessor implements Processor {

    private static final Logger log = LoggerFactory.getLogger(EventDeduplicationProcessor.class);

    private final Optional<EventDeduplicationService> deduplication;

    EventDeduplicationProcessor(Optional<EventDeduplicationService> deduplication) {
        this.deduplication = deduplication;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void process(Exchange exchange) {
        List<Event> events = exchange.getMessage().getBody(List.class);
        String routeId = exchange.getFromRouteId();
        log.info("{} found {} events", routeId, events.size());

        List<Event> newEvents = deduplication.map(d -> d.filter(events)).orElse(events);
        if (newEvents.size() < events.size()) {
            log.info("Deduplicated: {} new, {} already sent",
                    newEvents.size(), events.size() - newEvents.size());
        }
        exchange.getMessage().setBody(newEvents);
    }
}
