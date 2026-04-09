package com.d3bot.events.routes;

import com.d3bot.events.deduplicators.EventDeduplicationService;
import com.d3bot.events.models.Event;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import java.util.List;
import java.util.Optional;

class EventMarkSentProcessor implements Processor {

    private final Optional<EventDeduplicationService> deduplication;

    EventMarkSentProcessor(Optional<EventDeduplicationService> deduplication) {
        this.deduplication = deduplication;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void process(Exchange exchange) {
        List<Event> events = exchange.getMessage().getBody(List.class);
        deduplication.ifPresent(d -> d.markSent(events));
    }
}
