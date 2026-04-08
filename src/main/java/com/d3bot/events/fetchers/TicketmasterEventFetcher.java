package com.d3bot.events.fetchers;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Component
public class TicketmasterEventFetcher {

    static final String BASE_URL = "https://app.ticketmaster.com/discovery/v2/events.json";

    private final HttpClient httpClient;

    public TicketmasterEventFetcher(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public String fetch(String venueId, String apiKey) throws IOException, InterruptedException {
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
