package com.d3bot.events.notifiers;

import com.d3bot.events.models.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty("heartbeat.targets[0].url")
public class HeartbeatEventNotifier implements EventNotifier {

    private static final Logger log = LoggerFactory.getLogger(HeartbeatEventNotifier.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    /**
     * A single heartbeat target.
     *
     * <ul>
     *   <li>{@code method} — {@code GET} (default) or {@code POST}</li>
     *   <li>{@code token} — when present, added as {@code Authorization: Bearer <token>}</li>
     * </ul>
     *
     * Example (plain GET — healthchecks.io, Dead Man's Snitch):
     * <pre>heartbeat.targets[0].url=https://hc-ping.com/abc123</pre>
     *
     * Example (POST — Gatus external-endpoint push API):
     * <pre>
     * heartbeat.targets[1].url=https://gatus.example.com/api/v1/endpoints/jobs_gig-hub/external?success=true
     * heartbeat.targets[1].method=POST
     * heartbeat.targets[1].token=${GATUS_TOKEN}
     * </pre>
     */
    public record HeartbeatTarget(String url, String method, String token) {
        public boolean isPost() {
            return "POST".equalsIgnoreCase(method);
        }

        public boolean hasToken() {
            return token != null && !token.isBlank();
        }
    }

    private final HttpClient httpClient;
    private final List<HeartbeatTarget> targets;

    public HeartbeatEventNotifier(HttpClient httpClient, Environment environment) {
        this.httpClient = httpClient;
        this.targets = Binder.get(environment)
                .bind("heartbeat.targets", Bindable.listOf(Map.class))
                .orElse(List.of())
                .stream()
                .map(m -> new HeartbeatTarget(
                        (String) m.get("url"),
                        (String) m.getOrDefault("method", "GET"),
                        (String) m.get("token")))
                .filter(t -> t.url() != null && !t.url().isBlank())
                .toList();
    }

    @Override
    public void notify(List<Event> events) {
        targets.forEach(this::ping);
    }

    private void ping(HeartbeatTarget target) {
        try {
            HttpRequest request = target.isPost() ? buildPost(target) : buildGet(target);
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            int status = response.statusCode();
            String method = target.isPost() ? "POST" : "GET";

            if (status >= 200 && status < 300) {
                log.info("Heartbeat {} {} → {}", method, target.url(), status);
            } else {
                log.warn("Heartbeat {} {} returned non-2xx status: {}", method, target.url(), status);
            }
        } catch (IOException | InterruptedException e) {
            log.error("Heartbeat failed for {}: {}", target.url(), e.getMessage());
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private HttpRequest buildGet(HeartbeatTarget target) {
        return HttpRequest.newBuilder()
                .uri(URI.create(target.url()))
                .timeout(TIMEOUT)
                .GET()
                .build();
    }

    private HttpRequest buildPost(HeartbeatTarget target) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(target.url()))
                .timeout(TIMEOUT)
                .POST(HttpRequest.BodyPublishers.noBody());
        if (target.hasToken()) {
            builder.header("Authorization", "Bearer " + target.token());
        }
        return builder.build();
    }
}
