package com.d3bot.events.utilities;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * Fetches pages through a <a href="https://github.com/FlareSolverr/FlareSolverr">FlareSolverr</a>
 * instance, which loads them in a headless browser. Used for sites that sit behind a
 * Cloudflare browser check and reject plain HTTP clients.
 */
@Component
@ConditionalOnProperty("fetchers.flaresolverr.url")
public class FlareSolverrClient {

    static final int MAX_TIMEOUT_MS = 120_000;

    private final HttpClient httpClient;
    private final URI endpoint;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public FlareSolverrClient(HttpClient httpClient, @Value("${fetchers.flaresolverr.url}") String endpoint) {
        this.httpClient = httpClient;
        this.endpoint = URI.create(endpoint);
    }

    public String fetch(String url) throws IOException, InterruptedException {
        String body = objectMapper.writeValueAsString(
                Map.of("cmd", "request.get", "url", url, "maxTimeout", MAX_TIMEOUT_MS));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(endpoint)
                .timeout(Duration.ofMillis(MAX_TIMEOUT_MS + 30_000L))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("FlareSolverr returned status " + response.statusCode() + ": " + response.body());
        }

        JsonNode json = objectMapper.readTree(response.body());
        if (!"ok".equals(json.path("status").asText())) {
            throw new IOException("FlareSolverr failed for " + url + ": " + json.path("message").asText());
        }
        JsonNode solution = json.path("solution");
        int status = solution.path("status").asInt();
        if (status != 200) {
            throw new IOException("Request via FlareSolverr failed with status " + status + " for " + url);
        }
        return solution.path("response").asText();
    }
}
