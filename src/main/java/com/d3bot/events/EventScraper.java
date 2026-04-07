package com.d3bot.events;

import java.io.IOException;
import java.util.List;

public interface EventScraper {
    List<Event> scrape() throws IOException;
}
