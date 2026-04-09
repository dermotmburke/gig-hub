package com.d3bot.events.fetchers;

import com.d3bot.events.utilities.UrlFetcher;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class BanquetEventFetcherTest {

    private final UrlFetcher urlFetcher = mock(UrlFetcher.class);
    private final BanquetEventFetcher fetcher =
            new BanquetEventFetcher(urlFetcher, "https://www.banquetrecords.com/events?w=1000");

    @Test
    void fetchDelegatesToUrlFetcherWithConfiguredUrl() throws Exception {
        fetcher.fetch();
        verify(urlFetcher).fetch("https://www.banquetrecords.com/events?w=1000");
    }
}
