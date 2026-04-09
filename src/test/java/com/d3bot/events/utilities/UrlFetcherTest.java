package com.d3bot.events.utilities;

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

class UrlFetcherTest {

    private final HttpClient httpClient = mock(HttpClient.class);
    private final UrlFetcher urlFetcher = new UrlFetcher(httpClient);

    @Test
    @SuppressWarnings("unchecked")
    void fetchReturnsResponseBodyOn200() throws Exception {
        HttpResponse<String> response = mock(HttpResponse.class);
        doReturn(200).when(response).statusCode();
        doReturn("body content").when(response).body();
        doReturn(response).when(httpClient).send(any(HttpRequest.class), any());

        assertEquals("body content", urlFetcher.fetch("https://example.com"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void fetchThrowsIOExceptionOnNon200() throws Exception {
        HttpResponse<String> response = mock(HttpResponse.class);
        doReturn(401).when(response).statusCode();
        doReturn("Unauthorized").when(response).body();
        doReturn(response).when(httpClient).send(any(HttpRequest.class), any());

        assertThrows(IOException.class, () -> urlFetcher.fetch("https://example.com"));
    }
}
