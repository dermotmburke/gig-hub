package com.d3bot.events.pipelines;

import com.d3bot.events.deduplication.EventDeduplicationService;
import com.d3bot.events.extractors.RoyalAlbertHallExtractor;
import com.d3bot.events.fetchers.TicketmasterEventFetcher;
import com.d3bot.events.models.Event;
import com.d3bot.events.notifiers.EventNotifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
@ConditionalOnProperty({"ticketmaster.api-key", "ticketmaster.venues.royalalberthall.id"})
public class RoyalAlbertHallEventPipeline extends EventPipeline {

    private final TicketmasterEventFetcher fetcher;
    private final RoyalAlbertHallExtractor extractor;
    private final String apiKey;
    private final String venueId;

    public RoyalAlbertHallEventPipeline(
            TicketmasterEventFetcher fetcher,
            RoyalAlbertHallExtractor extractor,
            @Value("${ticketmaster.api-key}") String apiKey,
            @Value("${ticketmaster.venues.royalalberthall.id}") String venueId,
            List<EventNotifier> notifiers,
            Optional<EventDeduplicationService> deduplication) {
        super(notifiers, deduplication);
        this.fetcher = fetcher;
        this.extractor = extractor;
        this.apiKey = apiKey;
        this.venueId = venueId;
    }

    @Override
    protected String fetch() throws IOException, InterruptedException {
        return fetcher.fetch(venueId, apiKey);
    }

    @Override
    protected List<Event> extract(String json) throws IOException {
        return extractor.extract(json);
    }
}
