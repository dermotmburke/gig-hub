package com.d3bot.events.pipelines;

import com.d3bot.events.deduplication.EventDeduplicationService;
import com.d3bot.events.extractors.BanquetEventExtractor;
import com.d3bot.events.fetchers.EventFetcher;
import com.d3bot.events.models.Event;
import com.d3bot.events.notifiers.EventNotifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
public class BanquetEventPipeline extends EventPipeline {

    private final EventFetcher fetcher;
    private final BanquetEventExtractor extractor;

    @Value("${scrapers.banquet.url:https://www.banquetrecords.com/events?w=1000}")
    private String url;

    public BanquetEventPipeline(
            EventFetcher fetcher,
            BanquetEventExtractor extractor,
            List<EventNotifier> notifiers,
            Optional<EventDeduplicationService> deduplication) {
        super(notifiers, deduplication);
        this.fetcher = fetcher;
        this.extractor = extractor;
    }

    @Override
    protected String fetch() throws IOException {
        return fetcher.fetch(url);
    }

    @Override
    protected List<Event> extract(String html) {
        return extractor.extract(html);
    }
}
