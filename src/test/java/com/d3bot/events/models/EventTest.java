package com.d3bot.events.models;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class EventTest {

    private final Event event = new Event("Artist", "Venue", LocalDateTime.of(2026, 4, 7, 19, 0), "/url");

    @Test
    void checksumDoesNotThrow() {
        assertDoesNotThrow(event::checksum);
    }

    @Test
    void checksumIsStable() {
        assertEquals(event.checksum(), event.checksum());
    }

    @Test
    void keyHasBanquetPrefix() {
        assertTrue(event.key().startsWith("banquet:event:"));
    }

    @Test
    void eventsWithSameVenueAndDateButDifferentTimeHaveDistinctKeys() {
        var matinee = new Event("Artist", "Venue", LocalDateTime.of(2026, 4, 7, 14, 0), "/url");
        var evening = new Event("Artist", "Venue", LocalDateTime.of(2026, 4, 7, 19, 0), "/url");
        assertNotEquals(matinee.key(), evening.key());
    }
}
