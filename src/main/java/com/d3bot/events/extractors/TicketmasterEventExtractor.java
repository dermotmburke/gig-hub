package com.d3bot.events.extractors;

import com.d3bot.events.models.Event;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class TicketmasterEventExtractor {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<Event> extract(String json) throws IOException {
        JsonNode root = objectMapper.readTree(json);
        JsonNode embedded = root.path("_embedded");
        if (embedded.isMissingNode()) {
            return List.of();
        }
        JsonNode eventsNode = embedded.path("events");
        if (eventsNode.isMissingNode() || !eventsNode.isArray()) {
            return List.of();
        }
        List<Event> events = new ArrayList<>();
        for (JsonNode eventNode : eventsNode) {
            Event event = parseEvent(eventNode);
            if (event != null) {
                events.add(event);
            }
        }
        return events;
    }

    private Event parseEvent(JsonNode eventNode) {
        String name = eventNode.path("name").asText(null);
        String url = eventNode.path("url").asText("");

        JsonNode startNode = eventNode.path("dates").path("start");
        String localDate = startNode.path("localDate").asText(null);

        if (name == null || localDate == null) {
            return null;
        }

        String localTime = startNode.path("localTime").asText(null);
        LocalDate date = LocalDate.parse(localDate);
        LocalTime time = (localTime != null && !localTime.isBlank())
                ? LocalTime.parse(localTime)
                : LocalTime.MIDNIGHT;

        JsonNode venuesNode = eventNode.path("_embedded").path("venues");
        String venueName = (venuesNode.isArray() && !venuesNode.isEmpty())
                ? venuesNode.get(0).path("name").asText("Unknown Venue")
                : "Unknown Venue";

        return new Event(name, venueName, LocalDateTime.of(date, time), url);
    }
}
