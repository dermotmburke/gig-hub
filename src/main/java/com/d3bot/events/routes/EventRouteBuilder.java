package com.d3bot.events.routes;

import com.d3bot.events.deduplicators.EventDeduplicationService;
import com.d3bot.events.extractors.EventExtractor;
import com.d3bot.events.fetchers.EventFetcher;
import com.d3bot.events.notifiers.EventNotifier;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;

import java.util.List;
import java.util.Optional;

/**
 * Base Camel route builder for venue event pipelines. Subclasses wire together a
 * venue-specific {@link EventFetcher} and {@link EventExtractor}; the invariant
 * route definition — fetch, extract, deduplicate, notify, mark sent — is owned here.
 *
 * <p>Each route is exposed as a {@code direct:<routeId>} endpoint, triggered by
 * the {@link com.d3bot.events.runners.EventPipelineRunner}.
 */
public abstract class EventRouteBuilder extends RouteBuilder {

    private final String routeId;
    private final EventFetcher fetcher;
    private final EventExtractor extractor;
    private final List<EventNotifier> notifiers;
    private final Optional<EventDeduplicationService> deduplication;

    protected EventRouteBuilder(
            EventFetcher fetcher,
            EventExtractor extractor,
            List<EventNotifier> notifiers,
            Optional<EventDeduplicationService> deduplication) {
        this.routeId = deriveRouteId(getClass());
        this.fetcher = fetcher;
        this.extractor = extractor;
        this.notifiers = notifiers;
        this.deduplication = deduplication;
    }

    static String deriveRouteId(Class<?> clazz) {
        String name = clazz.getSimpleName().replace("EventRouteBuilder", "");
        return name.replaceAll("([a-z])([A-Z])", "$1-$2").toLowerCase() + "-pipeline";
    }

    public String getRouteId() {
        return routeId;
    }

    @Override
    public void configure() {
        onException(InterruptedException.class)
                .handled(true)
                .process(exchange -> Thread.currentThread().interrupt())
                .log(LoggingLevel.ERROR, "Pipeline interrupted for " + routeId);

        onException(Exception.class)
                .handled(true)
                .log(LoggingLevel.ERROR, "Pipeline failed for " + routeId + ": ${exception.message}");

        from("direct:" + routeId)
                .routeId(routeId)
                .bean(fetcher)
                .bean(extractor)
                .process(new EventDeduplicationProcessor(deduplication))
                .process(new EventNotificationProcessor(notifiers))
                .process(new EventMarkSentProcessor(deduplication));
    }
}
