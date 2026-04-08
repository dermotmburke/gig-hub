package com.d3bot.events.pipelines;

import com.d3bot.events.deduplicators.EventDeduplicationService;
import com.d3bot.events.extractors.EventExtractor;
import com.d3bot.events.fetchers.EventFetcher;
import com.d3bot.events.models.Event;
import com.d3bot.events.notifiers.EventNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * Base class for venue event pipelines. Subclasses wire together a venue-specific
 * {@link EventFetcher} and {@link EventExtractor}; the invariant lifecycle —
 * fetch, extract, deduplicate, notify, mark sent — is owned here.
 *
 * <p>To add a new Ticketmaster venue:
 *
 * <pre>{@code
 * @Component
 * @ConditionalOnProperty({"fetchers.ticketmaster.api-key", "fetchers.ticketmaster.venues.my-venue.id"})
 * public class MyVenueEventPipeline extends EventPipeline {
 *
 *     public MyVenueEventPipeline(
 *             MyVenueEventFetcher fetcher,
 *             MyVenueExtractor extractor,
 *             List<EventNotifier> notifiers,
 *             Optional<EventDeduplicationService> deduplication) {
 *         super(fetcher, extractor, notifiers, deduplication);
 *     }
 * }
 * }</pre>
 */
public abstract class EventPipeline {

    private static final Logger log = LoggerFactory.getLogger(EventPipeline.class);

    private final EventFetcher fetcher;
    private final EventExtractor extractor;
    private final List<EventNotifier> notifiers;
    private final Optional<EventDeduplicationService> deduplication;

    protected EventPipeline(
            EventFetcher fetcher,
            EventExtractor extractor,
            List<EventNotifier> notifiers,
            Optional<EventDeduplicationService> deduplication) {
        this.fetcher = fetcher;
        this.extractor = extractor;
        this.notifiers = notifiers;
        this.deduplication = deduplication;
    }

    public final void run() {
        try {
            String raw = fetcher.fetch();
            List<Event> events = extractor.extract(raw);
            log.info("{} found {} events", getClass().getSimpleName(), events.size());

            List<Event> newEvents = deduplication.map(d -> d.filter(events)).orElse(events);
            if (newEvents.size() < events.size()) {
                log.info("Deduplicated: {} new, {} already sent", newEvents.size(), events.size() - newEvents.size());
            }

            notifiers.forEach(n -> n.notify(newEvents));
            deduplication.ifPresent(d -> d.markSent(newEvents));

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Pipeline interrupted for {}", getClass().getSimpleName());
        } catch (Exception e) {
            log.error("Pipeline failed for {}: {}", getClass().getSimpleName(), e.getMessage());
        }
    }
}
