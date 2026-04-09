package com.d3bot.events.routes;

import com.d3bot.events.models.Event;
import com.d3bot.events.notifiers.EventNotifier;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import java.util.List;

class EventNotificationProcessor implements Processor {

    private final List<EventNotifier> notifiers;

    EventNotificationProcessor(List<EventNotifier> notifiers) {
        this.notifiers = notifiers;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void process(Exchange exchange) {
        List<Event> events = exchange.getMessage().getBody(List.class);
        notifiers.forEach(n -> n.notify(events));
    }
}
