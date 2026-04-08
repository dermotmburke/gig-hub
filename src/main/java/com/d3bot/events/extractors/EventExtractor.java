package com.d3bot.events.extractors;

import com.d3bot.events.models.Event;

import java.io.IOException;
import java.util.List;

public interface EventExtractor {
    List<Event> extract(String raw) throws IOException;
}
