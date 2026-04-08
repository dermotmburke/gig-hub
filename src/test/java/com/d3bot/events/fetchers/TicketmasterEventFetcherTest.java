package com.d3bot.events.fetchers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class TicketmasterEventFetcherTest {

    private final HttpClient httpClient = mock(HttpClient.class);
    private final TicketmasterEventFetcher fetcher = new TicketmasterEventFetcher(httpClient);

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws Exception {
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        doReturn(200).when(mockResponse).statusCode();
        doReturn("{\"_embedded\":{}}").when(mockResponse).body();
        doReturn(mockResponse).when(httpClient).send(any(HttpRequest.class), any());
    }

    @Test
    void fetchSendsGetRequestToTicketmasterApi() throws Exception {
        fetcher.fetch("KovZpZAEdntA", "test-api-key");

        verify(httpClient).send(any(HttpRequest.class), any());
    }

    @Test
    void fetchReturnsResponseBody() throws Exception {
        String body = fetcher.fetch("KovZpZAEdntA", "test-api-key");

        assertEquals("{\"_embedded\":{}}", body);
    }

    @Test
    @SuppressWarnings("unchecked")
    void fetchThrowsIOExceptionOnNon200Status() throws Exception {
        HttpResponse<String> errorResponse = mock(HttpResponse.class);
        doReturn(401).when(errorResponse).statusCode();
        doReturn("Unauthorized").when(errorResponse).body();
        doReturn(errorResponse).when(httpClient).send(any(HttpRequest.class), any());

        assertThrows(IOException.class, () -> fetcher.fetch("KovZpZAEdntA", "bad-key"));
    }
}
