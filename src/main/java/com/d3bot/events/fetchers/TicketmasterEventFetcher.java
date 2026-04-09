package com.d3bot.events.fetchers;

import com.d3bot.events.utilities.UrlFetcher;

import java.io.IOException;

/**
 * Base class for Ticketmaster API fetchers. Subclasses supply the venue ID and API key
 * via their constructor.
 *
 * <p>To add a new Ticketmaster venue fetcher:
 *
 * <pre>{@code
 * @Component
 * @ConditionalOnProperty({"fetchers.ticketmaster.api-key", "fetchers.ticketmaster.venues.my-venue.id"})
 * public class MyVenueEventFetcher extends TicketmasterEventFetcher {
 *
 *     public MyVenueEventFetcher(
 *             UrlFetcher urlFetcher,
 *             @Value("${fetchers.ticketmaster.api-key}") String apiKey,
 *             @Value("${fetchers.ticketmaster.venues.my-venue.id}") String venueId) {
 *         super(urlFetcher, venueId, apiKey);
 *     }
 * }
 * }</pre>
 */
public abstract class TicketmasterEventFetcher implements EventFetcher {

    static final String BASE_URL = "https://app.ticketmaster.com/discovery/v2/events.json";

    private final UrlFetcher urlFetcher;
    private final String venueId;
    private final String apiKey;

    protected TicketmasterEventFetcher(UrlFetcher urlFetcher, String venueId, String apiKey) {
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
