package com.d3bot.events.routes;

import com.d3bot.events.deduplicators.EventDeduplicator;
import com.d3bot.events.extractors.EventExtractor;
import com.d3bot.events.fetchers.EventFetcher;
import com.d3bot.events.notifiers.EventNotifier;
import com.d3bot.events.routes.processors.EventDeduplicatorProcessor;
import com.d3bot.events.routes.processors.EventExtractorProcessor;
import com.d3bot.events.routes.processors.EventFetchProcessor;
import com.d3bot.events.routes.processors.EventMarkSentProcessor;
import com.d3bot.events.routes.processors.EventNotificationProcessor;
import com.d3bot.events.utilities.RouteIdBuilder;
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
    private final EventFetchProcessor fetchProcessor;
    private final EventExtractorProcessor extractorProcessor;
    private final EventDeduplicatorProcessor deduplicatorProcessor;
    private final EventNotificationProcessor notificationProcessor;
    private final EventMarkSentProcessor markSentProcessor;

    protected EventRouteBuilder(
            EventFetcher fetcher,
            EventExtractor extractor,
            List<EventNotifier> notifiers,
            Optional<EventDeduplicator> deduplicator) {
        this.routeId = RouteIdBuilder.build(getClass());
        this.fetchProcessor = new EventFetchProcessor(fetcher);
        this.extractorProcessor = new EventExtractorProcessor(extractor);
        this.deduplicatorProcessor = new EventDeduplicatorProcessor(deduplicator);
        this.notificationProcessor = new EventNotificationProcessor(notifiers);
        this.markSentProcessor = new EventMarkSentProcessor(deduplicator);
    }

    public String getRouteId() {
        return routeId;
    }

    @Override
    public void configure() {
        onException(Exception.class)
                .handled(true)
                .log(LoggingLevel.ERROR, "Pipeline failed for " + routeId + ": ${exception.message}");

        from("direct:" + routeId)
                .routeId(routeId)
                .process(fetchProcessor)
                .process(extractorProcessor)
                .process(deduplicatorProcessor)
                .process(notificationProcessor)
                .process(markSentProcessor);
    }
}
