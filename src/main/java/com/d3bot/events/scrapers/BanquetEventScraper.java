package com.d3bot.events.scrapers;

import com.d3bot.events.models.Event;
import com.d3bot.events.fetchers.EventFetcher;
import com.d3bot.events.extractors.EventExtractor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
public class BanquetEventScraper implements EventScraper {

    private final EventFetcher eventFetcher;
    private final EventExtractor eventExtractor;

    @Value("${scrapers.banquet.url:https://www.banquetrecords.com/events?w=1000}")
    private String url;

    public BanquetEventScraper(EventFetcher eventFetcher, EventExtractor eventExtractor) {
        this.eventFetcher = eventFetcher;
        this.eventExtractor = eventExtractor;
    }

    @Override
    public List<Event> scrape() throws IOException {
        return eventExtractor.extract(eventFetcher.fetch(url));
    }
}
