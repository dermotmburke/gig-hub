package com.d3bot.events.routes;

import com.d3bot.events.deduplicators.EventDeduplicationService;
import com.d3bot.events.extractors.EventExtractor;
import com.d3bot.events.fetchers.EventFetcher;
import com.d3bot.events.models.Event;
import com.d3bot.events.notifiers.EventNotifier;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger log = LoggerFactory.getLogger(EventRouteBuilder.class);

    private final String routeId;
    private final EventFetcher fetcher;
    private final EventExtractor extractor;
    private final List<EventNotifier> notifiers;
    private final Optional<EventDeduplicationService> deduplication;

    protected EventRouteBuilder(
            String routeId,
            EventFetcher fetcher,
            EventExtractor extractor,
            List<EventNotifier> notifiers,
            Optional<EventDeduplicationService> deduplication) {
        this.routeId = routeId;
        this.fetcher = fetcher;
        this.extractor = extractor;
        this.notifiers = notifiers;
        this.deduplication = deduplication;
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
                .bean(fetcher, "fetch")
                .bean(extractor, "extract")
                .process(this::deduplicate)
                .process(this::notifyAll)
                .process(this::markSent);
    }

    @SuppressWarnings("unchecked")
    private void deduplicate(Exchange exchange) {
        List<Event> events = exchange.getMessage().getBody(List.class);
        log.info("{} found {} events", routeId, events.size());

        List<Event> newEvents = deduplication.map(d -> d.filter(events)).orElse(events);
        if (newEvents.size() < events.size()) {
            log.info("Deduplicated: {} new, {} already sent",
                    newEvents.size(), events.size() - newEvents.size());
        }
        exchange.getMessage().setBody(newEvents);
    }

    @SuppressWarnings("unchecked")
    private void notifyAll(Exchange exchange) {
        List<Event> events = exchange.getMessage().getBody(List.class);
        notifiers.forEach(n -> n.notify(events));
    }

    @SuppressWarnings("unchecked")
    private void markSent(Exchange exchange) {
        List<Event> events = exchange.getMessage().getBody(List.class);
        deduplication.ifPresent(d -> d.markSent(events));
    }
}
