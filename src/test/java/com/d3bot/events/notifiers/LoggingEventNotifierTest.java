package com.d3bot.events.notifiers;

import com.d3bot.events.models.Event;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

class LoggingEventNotifierTest {

    private final LoggingEventNotifier notifier = new LoggingEventNotifier();

    @Test
    void notifyLogsEachEvent() {
        notifier.notify(List.of(
                new Event("The Cure", "O2 Arena", LocalDateTime.of(2026, 5, 10, 19, 0), "https://example.com/a"),
                new Event("Radiohead", "Wembley", LocalDateTime.of(2026, 5, 11, 20, 0), "https://example.com/b")
        ));
    }

    @Test
    void notifyHandlesEmptyList() {
        notifier.notify(List.of());
    }
}
