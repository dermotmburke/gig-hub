package com.d3bot.events;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@RestController
public class EventController {

    private static final String EVENTS_URL = "https://www.banquetrecords.com/events?w=1000";

    private final EventExtractor eventExtractor;
    private final RestTemplate restTemplate;

    public EventController(EventExtractor eventExtractor, RestTemplate restTemplate) {
        this.eventExtractor = eventExtractor;
        this.restTemplate = restTemplate;
    }

    @GetMapping("/events")
    public List<Event> getEvents() {
        String html = restTemplate.getForObject(EVENTS_URL, String.class);
        return eventExtractor.extract(html);
    }
}
