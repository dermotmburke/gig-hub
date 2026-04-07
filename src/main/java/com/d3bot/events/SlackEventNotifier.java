package com.d3bot.events;

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
import java.util.stream.Collectors;

@Component
@ConditionalOnProperty("slack.webhook-url")
public class SlackEventNotifier implements EventNotifier {

    private static final Logger log = LoggerFactory.getLogger(SlackEventNotifier.class);

    private final HttpClient httpClient;
    private final String webhookUrl;
    private final String channel;

    public SlackEventNotifier(
            HttpClient httpClient,
            @Value("${slack.webhook-url}") String webhookUrl,
            @Value("${slack.channel:#events}") String channel) {
        this.httpClient = httpClient;
        this.webhookUrl = webhookUrl;
        this.channel = channel;
    }

    @Override
    public void notify(List<Event> events) {
        if (events.isEmpty()) {
            return;
        }
        try {
            String payload = buildPayload(events);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.error("Slack notification failed with status {}: {}", response.statusCode(), response.body());
            } else {
                log.info("Notified Slack channel {} of {} events", channel, events.size());
            }
        } catch (IOException | InterruptedException e) {
            log.error("Failed to send Slack notification", e);
            Thread.currentThread().interrupt();
        }
    }

    String buildPayload(List<Event> events) {
        String eventList = events.stream()
                .map(e -> String.format("• *%s* — %s @ %s <%s|link>", e.artist(), e.date(), e.Location(), e.url()))
                .collect(Collectors.joining("\\n"));
        return String.format(
                "{\"channel\":\"%s\",\"text\":\"*%d upcoming events*\\n%s\"}",
                channel, events.size(), eventList
        );
    }
}
