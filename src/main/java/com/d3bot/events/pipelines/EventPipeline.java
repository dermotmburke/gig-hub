package com.d3bot.events.pipelines;

import com.d3bot.events.deduplication.EventDeduplicationService;
import com.d3bot.events.models.Event;
import com.d3bot.events.notifiers.EventNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Base class for venue event pipelines. Each subclass represents a single venue and
 * implements {@link #fetch()} and {@link #extract(String)} for its specific data source.
 * The invariant lifecycle — deduplicate, notify, mark sent — is owned here.
 *
 * <p>To add a new venue, extend this class and supply its fetcher, extractor, and config:
 *
 * <pre>{@code
 * @Component
 * @ConditionalOnProperty({"ticketmaster.api-key", "ticketmaster.venues.my-venue.id"})
 * public class MyVenueEventPipeline extends EventPipeline {
 *
 *     public MyVenueEventPipeline(
 *             TicketmasterEventFetcher fetcher,
 *             MyVenueExtractor extractor,
 *             @Value("${ticketmaster.api-key}") String apiKey,
 *             @Value("${ticketmaster.venues.my-venue.id}") String venueId,
 *             List<EventNotifier> notifiers,
 *             Optional<EventDeduplicationService> deduplication) {
 *         super(notifiers, deduplication);
 *         // ...
 *     }
 *
 *     @Override protected String fetch() throws IOException, InterruptedException { ... }
 *     @Override protected List<Event> extract(String raw) throws IOException { ... }
 * }
 * }</pre>
 */
public abstract class EventPipeline {

    private static final Logger log = LoggerFactory.getLogger(EventPipeline.class);

    private final List<EventNotifier> notifiers;
    private final Optional<EventDeduplicationService> deduplication;

    protected EventPipeline(List<EventNotifier> notifiers, Optional<EventDeduplicationService> deduplication) {
        this.notifiers = notifiers;
        this.deduplication = deduplication;
    }

    protected abstract String fetch() throws IOException, InterruptedException;

    protected abstract List<Event> extract(String raw) throws IOException;

    public final void run() {
        try {
            String raw = fetch();
            List<Event> events = extract(raw);
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
