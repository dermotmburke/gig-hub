package com.d3bot.events.fetchers;

import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class BanquetEventFetcher implements EventFetcher {

    private final String url;

    public BanquetEventFetcher(@Value("${fetchers.banquet.url:https://www.banquetrecords.com/events?w=1000}") String url) {
        this.url = url;
    }

    @Override
    public String fetch() throws IOException {
        return Jsoup.connect(url).execute().body();
    }
}
