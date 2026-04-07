package com.d3bot.events.extractors;

import com.d3bot.events.models.Event;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EventExtractorTest {

    static List<Event> events;

    @BeforeAll
    static void setup() throws IOException, URISyntaxException {
        var resource = EventExtractorTest.class.getClassLoader().getResource("events.html");
        String html = Files.readString(Path.of(resource.toURI()));
        events = new EventExtractor().extract(html);
    }

    @Test
    void extractsEvents() {
        assertFalse(events.isEmpty());
    }

    @Test
    void extractsExpectedNumberOfEvents() {
        assertEquals(47, events.size());
    }

    @Test
    void firstEventHasCorrectArtist() {
        assertEquals("Lightyear / Slow Gherkin", events.get(0).artist());
    }

    @Test
    void firstEventHasCorrectDate() {
        assertEquals("Monday 6th April", events.get(0).date());
    }

    @Test
    void firstEventHasCorrectLocation() {
        assertEquals("The Fighting Cocks", events.get(0).Location());
    }

    @Test
    void firstEventHasCorrectUrl() {
        assertTrue(events.get(0).url().contains("LSG060426"));
    }

    @Test
    void cardMissingArtistSpanIsSkipped() {
        String html = "<a class=\"card\" href=\"/event\">" +
                      "<span class=\"title\">Monday 6th April at The Venue, 7pm</span>" +
                      "</a>";
        assertEquals(0, new EventExtractor().extract(html).size());
    }

    @Test
    void cardMissingTitleSpanIsSkipped() {
        String html = "<a class=\"card\" href=\"/event\">" +
                      "<span class=\"artist\">Some Artist</span>" +
                      "</a>";
        assertEquals(0, new EventExtractor().extract(html).size());
    }

    @Test
    void cardMissingHrefIsSkipped() {
        String html = "<a class=\"card\">" +
                      "<span class=\"artist\">Some Artist</span>" +
                      "<span class=\"title\">Monday 6th April at The Venue, 7pm</span>" +
                      "</a>";
        assertEquals(0, new EventExtractor().extract(html).size());
    }

    @Test
    void cardMissingAtSeparatorInTitleIsSkipped() {
        String html = "<a class=\"card\" href=\"/event\">" +
                      "<span class=\"artist\">Some Artist</span>" +
                      "<span class=\"title\">Monday 6th April</span>" +
                      "</a>";
        assertEquals(0, new EventExtractor().extract(html).size());
    }
}
