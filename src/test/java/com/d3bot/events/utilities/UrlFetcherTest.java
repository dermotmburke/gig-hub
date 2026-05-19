package com.d3bot.events.utilities;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class UrlFetcherTest {

    private final HttpClient httpClient = mock(HttpClient.class);
    private final UrlFetcher urlFetcher = new UrlFetcher(httpClient);

    @Test
    @SuppressWarnings("unchecked")
    void fetchSendsGetRequestAndReturnsResponseBodyOn200() throws Exception {
        HttpResponse<String> response = mock(HttpResponse.class);
        doReturn(200).when(response).statusCode();
        doReturn("body content").when(response).body();
        doReturn(response).when(httpClient).send(any(HttpRequest.class), any());

        assertEquals("body content", urlFetcher.fetch("https://example.com/events?size=10"));

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(captor.capture(), any());
        assertEquals("GET", captor.getValue().method());
        assertEquals(URI.create("https://example.com/events?size=10"), captor.getValue().uri());
    }

    @Test
    @SuppressWarnings("unchecked")
    void fetchThrowsIOExceptionIncludingStatusAndBodyOnNon200() throws Exception {
        HttpResponse<String> response = mock(HttpResponse.class);
        doReturn(401).when(response).statusCode();
        doReturn("Unauthorized").when(response).body();
        doReturn(response).when(httpClient).send(any(HttpRequest.class), any());

        IOException exception = assertThrows(IOException.class, () -> urlFetcher.fetch("https://example.com"));

        assertTrue(exception.getMessage().contains("401"));
        assertTrue(exception.getMessage().contains("Unauthorized"));
    }
}
