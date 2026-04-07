package com.d3bot.events.fetchers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

class EventFetcherTest {

    @Test
    void fetchNoArgDelegatesToFetchWithConfiguredUrl() throws Exception {
        EventFetcher fetcher = spy(new EventFetcher("https://example.com"));
        doReturn("<html>").when(fetcher).fetch("https://example.com");

        assertEquals("<html>", fetcher.fetch());
        verify(fetcher).fetch("https://example.com");
    }
}
