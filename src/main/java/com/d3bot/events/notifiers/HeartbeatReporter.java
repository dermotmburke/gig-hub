package com.d3bot.events.notifiers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reports the outcome of a whole run (all pipelines) to heartbeat targets, once per run.
 *
 * <p>On success every target is pinged with its configured URL. On failure:
 * <ul>
 *   <li>targets whose URL carries a {@code success=} query parameter (e.g. Gatus external endpoints)
 *       are pinged with {@code success=false&error=<failed pipelines>}</li>
 *   <li>other targets (plain dead-man's-switch URLs) are not pinged, so the missed check-in raises the alert</li>
 * </ul>
 */
@Component
@ConditionalOnProperty("heartbeat.targets[0].url")
public class HeartbeatReporter {

    private static final Logger log = LoggerFactory.getLogger(HeartbeatReporter.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final Pattern SUCCESS_PARAM = Pattern.compile("([?&])success=[^&]*");

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
     * heartbeat.targets[1].token=${HEARTBEAT_TOKEN}
     * </pre>
     */
    public record HeartbeatTarget(String url, String method, String token) {
        public boolean hasToken() {
            return token != null && !token.isBlank();
        }
    }

    private final HttpClient httpClient;
    private final List<HeartbeatTarget> targets;

    public HeartbeatReporter(HttpClient httpClient, Environment environment) {
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

    /**
     * @param failedPipelines ids of pipelines that failed this run; empty means the run succeeded
     */
    public void report(List<String> failedPipelines) {
        if (failedPipelines.isEmpty()) {
            targets.forEach(t -> ping(t, t.url()));
            return;
        }
        String error = "Pipelines failed: " + String.join(", ", failedPipelines);
        for (HeartbeatTarget target : targets) {
            failureUrl(target.url(), error).ifPresentOrElse(
                    url -> ping(target, url),
                    () -> log.warn("Skipping heartbeat {} because the run failed ({})", target.url(), error));
        }
    }

    static Optional<String> failureUrl(String url, String error) {
        Matcher matcher = SUCCESS_PARAM.matcher(url);
        if (!matcher.find()) {
            return Optional.empty();
        }
        String replaced = matcher.replaceFirst(Matcher.quoteReplacement(matcher.group(1) + "success=false"));
        return Optional.of(replaced + "&error=" + URLEncoder.encode(error, StandardCharsets.UTF_8));
    }

    private void ping(HeartbeatTarget target, String url) {
        try {
            HttpResponse<Void> response = httpClient.send(buildRequest(target, url), HttpResponse.BodyHandlers.discarding());
            int status = response.statusCode();
            if (status >= 200 && status < 300) {
                log.info("Heartbeat {} {} → {}", target.method(), url, status);
            } else {
                log.warn("Heartbeat {} {} returned non-2xx status: {}", target.method(), url, status);
            }
        } catch (IOException | InterruptedException e) {
            log.error("Heartbeat failed for {}: {}", url, e.getMessage());
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private HttpRequest buildRequest(HeartbeatTarget target, String url) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(TIMEOUT)
                .method(target.method(), HttpRequest.BodyPublishers.noBody());
        if (target.hasToken()) {
            builder.header("Authorization", "Bearer " + target.token());
        }
        return builder.build();
    }
}
