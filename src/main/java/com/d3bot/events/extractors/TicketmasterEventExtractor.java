package com.d3bot.events.extractors;

import com.d3bot.events.models.Event;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Base class for Ticketmaster JSON extractors. Handles the standard Discovery
 * API response format. Subclasses can override the protected hook methods to
 * customise how artist names, venue names, and times are extracted for a
 * specific venue.
 *
 * <p>To add a venue-specific extractor:
 *
 * <pre>{@code
 * @Service
 * public class MyVenueExtractor extends TicketmasterEventExtractor {
 *
 *     @Override
 *     protected String extractVenueName(JsonNode eventNode) {
 *         return "My Venue";   // override API value with a canonical name
 *     }
 * }
 * }</pre>
 */
@Service
public class TicketmasterEventExtractor implements EventExtractor {

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

    protected Event parseEvent(JsonNode eventNode) {
        String name = extractArtistName(eventNode);
        String url = eventNode.path("url").asText("");

        JsonNode startNode = eventNode.path("dates").path("start");
        String localDate = startNode.path("localDate").asText(null);

        if (name == null || localDate == null) {
            return null;
        }

        LocalDate date = LocalDate.parse(localDate);
        LocalTime time = extractTime(startNode);
        String venueName = extractVenueName(eventNode);

        return new Event(name, venueName, LocalDateTime.of(date, time), url);
    }

    protected String extractArtistName(JsonNode eventNode) {
        return eventNode.path("name").asText(null);
    }

    protected String extractVenueName(JsonNode eventNode) {
        JsonNode venuesNode = eventNode.path("_embedded").path("venues");
        return (venuesNode.isArray() && !venuesNode.isEmpty())
                ? venuesNode.get(0).path("name").asText("Unknown Venue")
                : "Unknown Venue";
    }

    protected LocalTime extractTime(JsonNode startNode) {
        String localTime = startNode.path("localTime").asText(null);
        return (localTime != null && !localTime.isBlank())
                ? LocalTime.parse(localTime)
                : LocalTime.MIDNIGHT;
    }
}
