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
@ConditionalOnProperty("gatus.heartbeat-url")
public class GatusEventNotifier implements EventNotifier {

    private static final Logger log = LoggerFactory.getLogger(GatusEventNotifier.class);

    private final HttpClient httpClient;
    private final String heartbeatUrl;

    public GatusEventNotifier(
            HttpClient httpClient,
            @Value("${gatus.heartbeat-url}") String heartbeatUrl) {
        this.httpClient = httpClient;
        this.heartbeatUrl = heartbeatUrl;
    }

    @Override
    public void notify(List<Event> events) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(heartbeatUrl))
                    .GET()
                    .build();
            httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            log.info("Pinged Gatus heartbeat");
        } catch (IOException | InterruptedException e) {
            log.error("Failed to ping Gatus heartbeat: {}", e.getMessage());
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
