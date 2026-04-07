package com.d3bot.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LoggingEventNotifier implements EventNotifier {

    private static final Logger log = LoggerFactory.getLogger(LoggingEventNotifier.class);

    @Override
    public void notify(List<Event> events) {
        events.forEach(e -> log.info("Event: {}", e));
    }
}
