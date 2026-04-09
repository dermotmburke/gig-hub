package com.d3bot.events.processors;

import com.d3bot.events.deduplicators.EventDeduplicator;
import com.d3bot.events.models.Event;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import java.util.List;
import java.util.Optional;

public class EventMarkSentProcessor implements Processor {

    private final Optional<EventDeduplicator> deduplicator;

    public EventMarkSentProcessor(Optional<EventDeduplicator> deduplicator) {
        this.deduplicator = deduplicator;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void process(Exchange exchange) {
        List<Event> events = exchange.getMessage().getBody(List.class);
        deduplicator.ifPresent(d -> d.markSent(events));
    }
}
