package com.d3bot.events.routes.processors;

import com.d3bot.events.fetchers.EventFetcher;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

public class EventFetchProcessor implements Processor {

    private final EventFetcher fetcher;

    public EventFetchProcessor(EventFetcher fetcher) {
        this.fetcher = fetcher;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        try {
            exchange.getIn().setBody(fetcher.fetch());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            exchange.setRouteStop(true);
        }
    }
}
