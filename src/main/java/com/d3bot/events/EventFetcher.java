package com.d3bot.events;

import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class EventFetcher {

    private final String url;

    public EventFetcher(@Value("${scraper.url:https://www.banquetrecords.com/events?w=1000}") String url) {
        this.url = url;
    }

    public String fetch() throws IOException {
        return fetch(url);
    }

    public String fetch(String url) throws IOException {
        return Jsoup.connect(url).execute().body();
    }
}
