package com.d3bot.events.routes;

import com.d3bot.events.deduplicators.EventDeduplicationService;
import com.d3bot.events.extractors.BanquetEventExtractor;
import com.d3bot.events.fetchers.BanquetEventFetcher;
import com.d3bot.events.notifiers.EventNotifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class BanquetEventRouteBuilder extends EventRouteBuilder {

    public BanquetEventRouteBuilder(
            BanquetEventFetcher fetcher,
            BanquetEventExtractor extractor,
            List<EventNotifier> notifiers,
            Optional<EventDeduplicationService> deduplication) {
        super(fetcher, extractor, notifiers, deduplication);
    }
}
