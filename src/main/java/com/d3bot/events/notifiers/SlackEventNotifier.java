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
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Component
@ConditionalOnProperty("slack.webhook-url")
public class SlackEventNotifier implements EventNotifier {

    private static final Logger log = LoggerFactory.getLogger(SlackEventNotifier.class);

    private final HttpClient httpClient;
    private final String webhookUrl;

    public SlackEventNotifier(
            HttpClient httpClient,
            @Value("${slack.webhook-url}") String webhookUrl) {
        this.httpClient = httpClient;
        this.webhookUrl = webhookUrl;
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
                log.info("Notified Slack of {} events", events.size());
            }
        } catch (IOException | InterruptedException e) {
            log.error("Failed to send Slack notification", e);
            Thread.currentThread().interrupt();
        }
    }

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.ENGLISH);

    String buildPayload(List<Event> events) {
        String eventList = events.stream()
                .map(e -> e.url().startsWith("http")
                        ? String.format("• *%s* — %s @ %s <%s|link>", e.artist(), e.dateTime().format(DATE_FORMATTER), e.location(), e.url())
                        : String.format("• *%s* — %s @ %s", e.artist(), e.dateTime().format(DATE_FORMATTER), e.location()))
                .collect(Collectors.joining("\n"));
        String text = String.format("*%d upcoming events*\n%s", events.size(), eventList);
        return "{\"blocks\":[{\"type\":\"section\",\"text\":{\"type\":\"mrkdwn\",\"text\":\"" + jsonEscape(text) + "\"}}]}";
    }

    private static String jsonEscape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
