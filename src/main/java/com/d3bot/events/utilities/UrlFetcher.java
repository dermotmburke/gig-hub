package com.d3bot.events.utilities;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Component
public class UrlFetcher {

    private final HttpClient httpClient;

    public UrlFetcher(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public String fetch(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Request failed with status " + response.statusCode() + ": " + response.body());
        }
        return response.body();
    }
}
