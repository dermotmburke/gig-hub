package com.d3bot.events.scrapers;

import com.d3bot.events.models.Event;

import java.io.IOException;
import java.util.List;

public interface EventScraper {
    List<Event> scrape() throws IOException;
}
