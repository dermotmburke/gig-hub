package com.d3bot.events.pipelines;

import com.d3bot.events.deduplicators.EventDeduplicator;
import com.d3bot.events.extractors.EventExtractor;
import com.d3bot.events.fetchers.EventFetcher;
import com.d3bot.events.models.Event;
import com.d3bot.events.notifiers.EventNotifier;
import com.d3bot.events.utilities.RouteIdBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * Base class for venue event pipelines. Subclasses wire together a venue-specific
 * {@link EventFetcher} and {@link EventExtractor}; the invariant lifecycle —
 * fetch, extract, deduplicate, notify, mark sent — is owned here.
 */
public abstract class EventPipeline {

    private static final Logger log = LoggerFactory.getLogger(EventPipeline.class);

    private final String pipelineId;
    private final EventFetcher fetcher;
    private final EventExtractor extractor;
    private final List<EventNotifier> notifiers;
    private final Optional<EventDeduplicator> deduplicator;

    protected EventPipeline(
            EventFetcher fetcher,
            EventExtractor extractor,
            List<EventNotifier> notifiers,
            Optional<EventDeduplicator> deduplicator) {
        this.pipelineId = RouteIdBuilder.build(getClass());
        this.fetcher = fetcher;
        this.extractor = extractor;
        this.notifiers = notifiers;
        this.deduplicator = deduplicator;
    }

    protected EventPipeline(
            String pipelineId,
            EventFetcher fetcher,
            EventExtractor extractor,
            List<EventNotifier> notifiers,
            Optional<EventDeduplicator> deduplicator) {
        this.pipelineId = pipelineId;
        this.fetcher = fetcher;
        this.extractor = extractor;
        this.notifiers = notifiers;
        this.deduplicator = deduplicator;
    }

    public String getPipelineId() {
        return pipelineId;
    }

    public final void run() {
        try {
            String raw = fetcher.fetch();
            List<Event> events = extractor.extract(raw);
            log.info("{} found {} events", pipelineId, events.size());

            List<Event> newEvents = deduplicator.map(d -> d.filter(events)).orElse(events);
            if (newEvents.size() < events.size()) {
                log.info("Deduplicated: {} new, {} already sent", newEvents.size(), events.size() - newEvents.size());
            }

            notifiers.forEach(n -> n.notify(newEvents));
            deduplicator.ifPresent(d -> d.markSent(newEvents));

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Pipeline interrupted for {}", pipelineId);
        } catch (Exception e) {
            log.error("Pipeline failed for {}: {}", pipelineId, e.getMessage());
        }
    }
}
