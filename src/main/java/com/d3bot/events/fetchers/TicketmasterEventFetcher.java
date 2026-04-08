package com.d3bot.events.fetchers;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

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
 *             HttpClient httpClient,
 *             @Value("${fetchers.ticketmaster.api-key}") String apiKey,
 *             @Value("${fetchers.ticketmaster.venues.my-venue.id}") String venueId) {
 *         super(httpClient, venueId, apiKey);
 *     }
 * }
 * }</pre>
 */
public abstract class TicketmasterEventFetcher implements EventFetcher {

    static final String BASE_URL = "https://app.ticketmaster.com/discovery/v2/events.json";

    private final HttpClient httpClient;
    private final String venueId;
    private final String apiKey;

    protected TicketmasterEventFetcher(HttpClient httpClient, String venueId, String apiKey) {
        this.httpClient = httpClient;
        this.venueId = venueId;
        this.apiKey = apiKey;
    }

    @Override
    public final String fetch() throws IOException, InterruptedException {
        String url = BASE_URL + "?venueId=" + venueId + "&apikey=" + apiKey + "&size=200";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Ticketmaster API returned status " + response.statusCode() + ": " + response.body());
        }
        return response.body();
    }
}
