package com.d3bot.events.fetchers;

import com.d3bot.events.utilities.UrlFetcher;

import java.io.IOException;

/**
 * Ticketmaster Discovery API fetcher. Constructed with a venue ID and API key; add new venues
 * via {@code fetchers.ticketmaster.venues.<name>.id} configuration — no new class required.
 */
public class TicketmasterEventFetcher implements EventFetcher {

    static final String BASE_URL = "https://app.ticketmaster.com/discovery/v2/events.json";

    private final UrlFetcher urlFetcher;
    private final String venueId;
    private final String apiKey;

    public TicketmasterEventFetcher(UrlFetcher urlFetcher, String venueId, String apiKey) {
        this.urlFetcher = urlFetcher;
        this.venueId = venueId;
        this.apiKey = apiKey;
    }

    @Override
    public final String fetch() throws IOException, InterruptedException {
        String url = BASE_URL + "?venueId=" + venueId + "&apikey=" + apiKey + "&size=200";
        return urlFetcher.fetch(url);
    }
}
