package com.d3bot.events.fetchers;

import com.d3bot.events.utilities.UrlFetcher;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TicketmasterEventFetcherTest {

    private final UrlFetcher urlFetcher = mock(UrlFetcher.class);
    private final TicketmasterEventFetcher fetcher =
            new TicketmasterEventFetcher(urlFetcher, "KovZpZAEdntA", "test-api-key");

    @Test
    void fetchDelegatesToUrlFetcherWithConstructedUrl() throws Exception {
        fetcher.fetch();
        verify(urlFetcher).fetch(
                "https://app.ticketmaster.com/discovery/v2/events.json?venueId=KovZpZAEdntA&apikey=test-api-key&size=200");
    }

    @Test
    void fetchReturnsResponseBody() throws Exception {
        when(urlFetcher.fetch(any())).thenReturn("{\"_embedded\":{}}");
        assertEquals("{\"_embedded\":{}}", fetcher.fetch());
    }

    @Test
    void fetchPropagatesApiNotFoundErrorFromUrlFetcher() throws Exception {
        when(urlFetcher.fetch(any()))
                .thenThrow(new IOException("Request failed with status 404: Not Found"));

        IOException exception = assertThrows(IOException.class, fetcher::fetch);

        assertTrue(exception.getMessage().contains("404"));
    }

    @Test
    void fetchPropagatesServerErrorFromUrlFetcher() throws Exception {
        when(urlFetcher.fetch(any()))
                .thenThrow(new IOException("Request failed with status 500: Server overloaded"));

        IOException exception = assertThrows(IOException.class, fetcher::fetch);

        assertTrue(exception.getMessage().contains("500"));
    }

    @Test
    void fetchPassesThroughMalformedJsonBodyWithoutParsing() throws Exception {
        String malformedJson = "{ \"totalElements\": 1, \"_embedded\": \"INVALID_JSON\" }";
        when(urlFetcher.fetch(any())).thenReturn(malformedJson);

        assertEquals(malformedJson, fetcher.fetch());
    }
}
