package com.d3bot.events.notifiers;

import com.d3bot.events.models.Event;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Component
@ConditionalOnProperty("slack.webhook-url")
public class SlackEventNotifier implements EventNotifier {

    private static final Logger log = LoggerFactory.getLogger(SlackEventNotifier.class);

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.ENGLISH);

    private final HttpClient httpClient;
    private final String webhookUrl;
    private final String gigSaverUrl;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SlackEventNotifier(
            HttpClient httpClient,
            @Value("${slack.webhook-url}") String webhookUrl,
            @Value("${gig-saver.url:}") String gigSaverUrl) {
        this.httpClient = httpClient;
        this.webhookUrl = webhookUrl;
        this.gigSaverUrl = gigSaverUrl;
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
                throw new RuntimeException("Slack notification failed with status " + response.statusCode() + ": " + response.body());
            }
            log.info("Notified Slack of {} events", events.size());
        } catch (IOException | InterruptedException e) {
            log.error("Failed to send Slack notification", e);
            Thread.currentThread().interrupt();
        }
    }

    String buildPayload(List<Event> events) {
        String eventList = events.stream()
                .map(e -> {
                    String line = e.url().startsWith("http")
                            ? String.format("• *<%s|%s>* — %s @ %s", e.url(), e.artist(), e.dateTime().format(DATE_FORMATTER), e.location())
                            : String.format("• *%s* — %s @ %s", e.artist(), e.dateTime().format(DATE_FORMATTER), e.location());
                    if (gigSaverUrl != null && !gigSaverUrl.isBlank()) {
                        String saveLink = gigSaverUrl + "/save"
                                + "?artist=" + URLEncoder.encode(e.artist(), StandardCharsets.UTF_8)
                                + "&location=" + URLEncoder.encode(e.location(), StandardCharsets.UTF_8)
                                + "&date=" + e.dateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                                + "&url=" + URLEncoder.encode(e.url(), StandardCharsets.UTF_8);
                        line = line + " | <" + saveLink + "|\uD83D\uDCBE Save>";
                    }
                    return line;
                })
                .collect(Collectors.joining("\n"));
        String text = String.format("*%d upcoming events*\n%s", events.size(), eventList);

        ObjectNode payload = objectMapper.createObjectNode().put("text", text);
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (IOException e) {
            throw new RuntimeException("Failed to build Slack payload", e);
        }
    }
}
