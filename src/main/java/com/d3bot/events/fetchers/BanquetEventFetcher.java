package com.d3bot.events.fetchers;

import com.d3bot.events.utilities.UrlFetcher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class BanquetEventFetcher implements EventFetcher {

    private final UrlFetcher urlFetcher;
    private final String url;

    public BanquetEventFetcher(
            UrlFetcher urlFetcher,
            @Value("${fetchers.banquet.url:https://www.banquetrecords.com/events?w=1000}") String url) {
        this.urlFetcher = urlFetcher;
        this.url = url;
    }

    @Override
    public String fetch() throws IOException, InterruptedException {
        return urlFetcher.fetch(url);
    }
}
