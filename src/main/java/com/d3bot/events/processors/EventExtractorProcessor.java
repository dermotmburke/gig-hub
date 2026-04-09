package com.d3bot.events.processors;

import com.d3bot.events.extractors.EventExtractor;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

public class EventExtractorProcessor implements Processor {

    private final EventExtractor extractor;

    public EventExtractorProcessor(EventExtractor extractor) {
        this.extractor = extractor;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        String raw = exchange.getIn().getBody(String.class);
        exchange.getIn().setBody(extractor.extract(raw));
    }
}
