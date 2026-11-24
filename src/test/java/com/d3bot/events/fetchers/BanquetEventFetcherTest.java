package com.d3bot.events.fetchers;

import com.d3bot.events.utilities.FlareSolverrClient;
import com.d3bot.events.utilities.UrlFetcher;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class BanquetEventFetcherTest {

    private static final String URL = "https://www.banquetrecords.com/events?w=1000";

    private final UrlFetcher urlFetcher = mock(UrlFetcher.class);
    private final FlareSolverrClient flareSolverr = mock(FlareSolverrClient.class);

    @Test
    void fetchDelegatesToUrlFetcherWhenFlareSolverrNotConfigured() throws Exception {
        new BanquetEventFetcher(urlFetcher, Optional.empty(), URL).fetch();
        verify(urlFetcher).fetch(URL);
    }

    @Test
    void fetchUsesFlareSolverrWhenConfigured() throws Exception {
        when(flareSolverr.fetch(URL)).thenReturn("<html/>");

        assertEquals("<html/>", new BanquetEventFetcher(urlFetcher, Optional.of(flareSolverr), URL).fetch());

        verifyNoInteractions(urlFetcher);
    }
}
