package com.d3bot.events.notifiers;

import com.d3bot.events.models.Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.env.MockEnvironment;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class HeartbeatEventNotifierTest {

    private final HttpClient httpClient = mock(HttpClient.class);
    private final MockEnvironment environment = new MockEnvironment()
            .withProperty("heartbeat.urls[0]", "https://monitor-a.example.com/ping")
            .withProperty("heartbeat.urls[1]", "https://monitor-b.example.com/ping");
    private final HeartbeatEventNotifier notifier = new HeartbeatEventNotifier(httpClient, environment);

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws Exception {
        HttpResponse<Void> mockResponse = mock(HttpResponse.class);
        doReturn(mockResponse).when(httpClient).send(any(HttpRequest.class), any());
    }

    @Test
    void notifyPingsAllConfiguredUrlsWithGetRequests() throws Exception {
        notifier.notify(List.of());

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient, times(2)).send(captor.capture(), any());

        List<HttpRequest> requests = captor.getAllValues();
        assertEquals(List.of(
                URI.create("https://monitor-a.example.com/ping"),
                URI.create("https://monitor-b.example.com/ping")),
                requests.stream().map(HttpRequest::uri).toList());
        assertEquals(List.of("GET", "GET"), requests.stream().map(HttpRequest::method).toList());
    }

    @Test
    void notifyPingsEvenWithEvents() throws Exception {
        notifier.notify(List.of(new Event("The Cure", "O2 Arena", LocalDateTime.of(2026, 5, 10, 19, 0), "https://example.com")));

        verify(httpClient, times(2)).send(any(HttpRequest.class), any());
    }

    @Test
    void notifyContinuesToPingRemainingUrlsAfterFailure() throws Exception {
        doThrow(new IOException("connection refused"))
                .doReturn(null)
                .when(httpClient).send(any(), any());

        notifier.notify(List.of());

        verify(httpClient, times(2)).send(any(HttpRequest.class), any());
    }

    @Test
    void notifyHandlesInterruptedExceptionAndRestoresInterruptFlag() throws Exception {
        doThrow(new InterruptedException()).when(httpClient).send(any(), any());

        notifier.notify(List.of());

        assertTrue(Thread.currentThread().isInterrupted());
        Thread.interrupted(); // clear for test cleanliness
    }
}
