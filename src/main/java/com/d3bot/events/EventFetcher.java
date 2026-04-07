package com.d3bot.events;

import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class EventFetcher {

    public String fetch(String url) throws IOException {
        return Jsoup.connect(url).execute().body();
    }
}
