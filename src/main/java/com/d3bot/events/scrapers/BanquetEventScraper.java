package com.d3bot.events.scrapers;

import com.d3bot.events.models.Event;
import com.d3bot.events.fetchers.EventFetcher;
import com.d3bot.events.extractors.BanquetEventExtractor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
public class BanquetEventScraper implements EventScraper {

    private final EventFetcher eventFetcher;
    private final BanquetEventExtractor banquetEventExtractor;

    @Value("${scrapers.banquet.url:https://www.banquetrecords.com/events?w=1000}")
    private String url;

    public BanquetEventScraper(EventFetcher eventFetcher, BanquetEventExtractor banquetEventExtractor) {
        this.eventFetcher = eventFetcher;
        this.banquetEventExtractor = banquetEventExtractor;
    }

    @Override
    public List<Event> scrape() throws IOException {
        return banquetEventExtractor.extract(eventFetcher.fetch(url));
    }
}
