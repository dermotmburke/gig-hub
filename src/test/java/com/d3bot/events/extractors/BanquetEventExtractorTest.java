package com.d3bot.events.extractors;

import com.d3bot.events.models.Event;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BanquetEventExtractorTest {

    // Fixed "today" so fixture dates don't depend on when the tests run
    static final Clock CLOCK = Clock.fixed(Instant.parse("2026-04-01T12:00:00Z"), ZoneOffset.UTC);
    static final BanquetEventExtractor EXTRACTOR = new BanquetEventExtractor(CLOCK);

    static List<Event> events;

    @BeforeAll
    static void setup() throws IOException, URISyntaxException {
        var resource = BanquetEventExtractorTest.class.getClassLoader().getResource("events.html");
        String html = Files.readString(Path.of(resource.toURI()));
        events = EXTRACTOR.extract(html);
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
    void firstEventHasCorrectDateTime() {
        assertEquals(LocalDateTime.of(2026,4, 6, 19, 0), events.get(0).dateTime());
    }

    @Test
    void firstEventHasCorrectLocation() {
        assertEquals("The Fighting Cocks", events.get(0).location());
    }

    @Test
    void firstEventHasCorrectUrl() {
        assertTrue(events.get(0).url().startsWith("https://www.banquetrecords.com"));
        assertTrue(events.get(0).url().contains("LSG060426"));
    }

    @Test
    void cardMissingArtistSpanIsSkipped() {
        String html = "<a class=\"card\" href=\"/event\">" +
                      "<span class=\"title\">Monday 6th April at The Venue, 7pm</span>" +
                      "</a>";
        assertEquals(0, EXTRACTOR.extract(html).size());
    }

    @Test
    void cardMissingTitleSpanIsSkipped() {
        String html = "<a class=\"card\" href=\"/event\">" +
                      "<span class=\"artist\">Some Artist</span>" +
                      "</a>";
        assertEquals(0, EXTRACTOR.extract(html).size());
    }

    @Test
    void cardMissingHrefIsSkipped() {
        String html = "<a class=\"card\">" +
                      "<span class=\"artist\">Some Artist</span>" +
                      "<span class=\"title\">Monday 6th April at The Venue, 7pm</span>" +
                      "</a>";
        assertEquals(0, EXTRACTOR.extract(html).size());
    }

    @Test
    void cardMissingAtSeparatorInTitleIsSkipped() {
        String html = "<a class=\"card\" href=\"/event\">" +
                      "<span class=\"artist\">Some Artist</span>" +
                      "<span class=\"title\">Monday 6th April</span>" +
                      "</a>";
        assertEquals(0, EXTRACTOR.extract(html).size());
    }

    @Test
    void cardMissingTimeDefaultsToMidnight() {
        String html = "<a class=\"card\" href=\"/event\">" +
                      "<span class=\"artist\">Some Artist</span>" +
                      "<span class=\"title\">Monday 6th April at The Venue</span>" +
                      "</a>";

        List<Event> result = EXTRACTOR.extract(html);

        assertEquals(1, result.size());
        assertEquals(LocalDateTime.of(2026,4, 6, 0, 0), result.get(0).dateTime());
    }

    @Test
    void cardWithInvalidMonthIsSkipped() {
        String html = "<a class=\"card\" href=\"/event\">" +
                      "<span class=\"artist\">Some Artist</span>" +
                      "<span class=\"title\">Monday 6th NotAMonth at The Venue, 7pm</span>" +
                      "</a>";

        assertEquals(0, EXTRACTOR.extract(html).size());
    }

    @Test
    void dateEarlierThanTodayRollsOverToNextYear() {
        String html = "<a class=\"card\" href=\"/event\">" +
                      "<span class=\"artist\">Some Artist</span>" +
                      "<span class=\"title\">Thursday 14th January at Circuit, 8:30pm</span>" +
                      "</a>";

        List<Event> result = EXTRACTOR.extract(html);

        assertEquals(LocalDateTime.of(2027, 1, 14, 20, 30), result.get(0).dateTime());
    }

    @Test
    void dateOfTodayStaysInCurrentYear() {
        String html = "<a class=\"card\" href=\"/event\">" +
                      "<span class=\"artist\">Some Artist</span>" +
                      "<span class=\"title\">Wednesday 1st April at The Venue, 7pm</span>" +
                      "</a>";

        List<Event> result = EXTRACTOR.extract(html);

        assertEquals(LocalDateTime.of(2026, 4, 1, 19, 0), result.get(0).dateTime());
    }
}
