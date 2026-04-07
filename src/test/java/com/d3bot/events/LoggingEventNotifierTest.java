package com.d3bot.events;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class LoggingEventNotifierTest {

    private final LoggingEventNotifier notifier = new LoggingEventNotifier();

    @Test
    void notifyLogsEachEvent() {
        List<Event> events = List.of(
                new Event("Artist A", "Venue A", "Monday", "/a"),
                new Event("Artist B", "Venue B", "Tuesday", "/b")
        );
        assertDoesNotThrow(() -> notifier.notify(events));
    }

    @Test
    void notifyWithEmptyListDoesNotThrow() {
        assertDoesNotThrow(() -> notifier.notify(List.of()));
    }
}
