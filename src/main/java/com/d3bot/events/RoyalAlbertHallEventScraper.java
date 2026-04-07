package com.d3bot.events;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
public class RoyalAlbertHallEventScraper implements EventScraper {

    private final EventFetcher eventFetcher;

    @Value("${scrapers.royal-albert-hall.url:https://www.royalalberthall.com/tickets/events/}")
    private String url;

    public RoyalAlbertHallEventScraper(EventFetcher eventFetcher) {
        this.eventFetcher = eventFetcher;
    }

    @Override
    public List<Event> scrape() throws IOException {
        String html = eventFetcher.fetch(url);
        // TODO: implement Royal Albert Hall specific parsing
        return List.of();
    }
}
