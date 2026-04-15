package com.d3bot.events.utilities;

import com.d3bot.events.models.Event;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class GigHubCalendarUrlBuilderTest {

    private static final Event BASIC_EVENT = new Event(
            "The Cure", "O2 Arena", LocalDateTime.of(2026, 5, 10, 19, 0), "https://example.com/event");

    @Test
    void buildsUrlWithCorrectPath() {
        String url = new GigHubCalendarUrlBuilder("http://localhost:3000").build(BASIC_EVENT);

        assertTrue(url.startsWith("http://localhost:3000/save?"));
    }

    @Test
    void includesUrlEncodedArtist() {
        var event = new Event("The Cure", "O2 Arena", LocalDateTime.of(2026, 5, 10, 19, 0), "https://example.com");
        String url = new GigHubCalendarUrlBuilder("http://localhost:3000").build(event);

        assertTrue(url.contains("artist=The+Cure"));
    }

    @Test
    void includesUrlEncodedLocation() {
        String url = new GigHubCalendarUrlBuilder("http://localhost:3000").build(BASIC_EVENT);

        assertTrue(url.contains("location=O2+Arena"));
    }

    @Test
    void includesIsoFormattedDate() {
        String url = new GigHubCalendarUrlBuilder("http://localhost:3000").build(BASIC_EVENT);

        assertTrue(url.contains("date=2026-05-10T19:00:00"));
    }

    @Test
    void includesUrlEncodedTicketUrl() {
        var event = new Event("Band", "Venue", LocalDateTime.of(2026, 5, 10, 19, 0), "https://example.com/event?id=123&ref=slack");
        String url = new GigHubCalendarUrlBuilder("http://localhost:3000").build(event);

        assertTrue(url.contains("url=https%3A%2F%2Fexample.com%2Fevent%3Fid%3D123%26ref%3Dslack"));
    }

    @Test
    void encodesSpecialCharactersInArtistName() {
        var event = new Event("AC/DC", "Wembley", LocalDateTime.of(2026, 6, 1, 20, 0), "https://example.com");
        String url = new GigHubCalendarUrlBuilder("http://localhost:3000").build(event);

        assertTrue(url.contains("artist=AC%2FDC"));
    }

    @Test
    void containsAllFourQueryParameters() {
        String url = new GigHubCalendarUrlBuilder("http://localhost:3000").build(BASIC_EVENT);

        assertTrue(url.contains("artist="));
        assertTrue(url.contains("location="));
        assertTrue(url.contains("date="));
        assertTrue(url.contains("url="));
    }
}
