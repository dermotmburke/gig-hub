package com.d3bot.events.extractors;

import com.d3bot.events.models.Event;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TicketmasterEventExtractorTest {

    static List<Event> events;

    @BeforeAll
    static void setup() throws IOException, URISyntaxException {
        var resource = TicketmasterEventExtractorTest.class.getClassLoader().getResource("ticketmaster-events.json");
        String json = Files.readString(Path.of(resource.toURI()));
        events = new RoyalAlbertHallExtractor().extract(json);
    }

    @Test
    void extractsEvents() {
        assertFalse(events.isEmpty());
    }

    @Test
    void extractsExpectedNumberOfEvents() {
        assertEquals(3, events.size());
    }

    @Test
    void firstEventHasCorrectArtist() {
        assertEquals("The National", events.get(0).artist());
    }

    @Test
    void firstEventHasCorrectDateTime() {
        assertEquals(LocalDateTime.of(2026, 6, 12, 19, 30), events.get(0).dateTime());
    }

    @Test
    void firstEventHasCorrectLocation() {
        assertEquals("O2 Academy Brixton", events.get(0).location());
    }

    @Test
    void firstEventHasCorrectUrl() {
        assertTrue(events.get(0).url().startsWith("https://www.ticketmaster.com"));
    }

    @Test
    void eventWithNoTimeFallsBackToMidnight() {
        assertEquals(LocalTime.MIDNIGHT, events.get(2).dateTime().toLocalTime());
    }

    @Test
    void returnsEmptyListWhenNoEmbeddedNode() throws IOException {
        List<Event> result = new RoyalAlbertHallExtractor().extract("{\"page\":{\"totalElements\":0}}");

        assertEquals(List.of(), result);
    }

    @Test
    void returnsEmptyListWhenEventsArrayMissing() throws IOException {
        List<Event> result = new RoyalAlbertHallExtractor().extract("{\"_embedded\":{}}");

        assertEquals(List.of(), result);
    }

    @Test
    void skipsEventWithMissingName() throws IOException {
        String json = "{\"_embedded\":{\"events\":[" +
                "{\"url\":\"https://example.com\",\"dates\":{\"start\":{\"localDate\":\"2026-06-01\"}}," +
                "\"_embedded\":{\"venues\":[{\"name\":\"Venue\"}]}}" +
                "]}}";

        assertEquals(List.of(), new RoyalAlbertHallExtractor().extract(json));
    }

    @Test
    void skipsEventWithMissingDate() throws IOException {
        String json = "{\"_embedded\":{\"events\":[" +
                "{\"name\":\"Artist\",\"url\":\"https://example.com\",\"dates\":{\"start\":{}}," +
                "\"_embedded\":{\"venues\":[{\"name\":\"Venue\"}]}}" +
                "]}}";

        assertEquals(List.of(), new RoyalAlbertHallExtractor().extract(json));
    }

    @Test
    void usesUnknownVenueWhenVenuesArrayMissing() throws IOException {
        String json = "{\"_embedded\":{\"events\":[" +
                "{\"name\":\"Artist\",\"url\":\"https://example.com\"," +
                "\"dates\":{\"start\":{\"localDate\":\"2026-06-01\"}}," +
                "\"_embedded\":{}}" +
                "]}}";

        List<Event> result = new RoyalAlbertHallExtractor().extract(json);
        assertEquals(1, result.size());
        assertEquals("Unknown Venue", result.get(0).location());
    }
}
