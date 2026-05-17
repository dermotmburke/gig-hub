package com.d3bot.events.notifiers;

import com.d3bot.events.models.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

@Component
@ConditionalOnProperty("heartbeat.urls")
public class HeartbeatEventNotifier implements EventNotifier {

    private static final Logger log = LoggerFactory.getLogger(HeartbeatEventNotifier.class);

    private final HttpClient httpClient;
    private final List<String> urls;

    public HeartbeatEventNotifier(
            HttpClient httpClient,
            @Value("${heartbeat.urls}") List<String> urls) {
        this.httpClient = httpClient;
        this.urls = urls;
    }

    @Override
    public void notify(List<Event> events) {
        urls.forEach(this::ping);
    }

    private void ping(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();
            httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            log.info("Pinged heartbeat: {}", url);
        } catch (IOException | InterruptedException e) {
            log.error("Failed to ping heartbeat {}: {}", url, e.getMessage());
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
