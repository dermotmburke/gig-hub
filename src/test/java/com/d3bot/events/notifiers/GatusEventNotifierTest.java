package com.d3bot.events.notifiers;

import com.d3bot.events.models.Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GatusEventNotifierTest {

    private final HttpClient httpClient = mock(HttpClient.class);
    private final GatusEventNotifier notifier = new GatusEventNotifier(httpClient, "https://status.example.com/api/v1/endpoints/gig-hub/heartbeat");

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws Exception {
        HttpResponse<Void> mockResponse = mock(HttpResponse.class);
        doReturn(200).when(mockResponse).statusCode();
        doReturn(mockResponse).when(httpClient).send(any(HttpRequest.class), any());
    }

    @Test
    void notifyPingsHeartbeatWithEvents() throws Exception {
        notifier.notify(List.of(new Event("The Cure", "O2 Arena", LocalDateTime.of(2026, 5, 10, 19, 0), "https://example.com")));

        verify(httpClient).send(any(HttpRequest.class), any());
    }

    @Test
    void notifyPingsHeartbeatEvenWithNoEvents() throws Exception {
        notifier.notify(List.of());

        verify(httpClient).send(any(HttpRequest.class), any());
    }

    @Test
    void notifyHandlesIOExceptionGracefully() throws Exception {
        doThrow(new IOException("connection refused")).when(httpClient).send(any(), any());

        notifier.notify(List.of());
    }

    @Test
    void notifyHandlesInterruptedException() throws Exception {
        doThrow(new InterruptedException()).when(httpClient).send(any(), any());

        notifier.notify(List.of());

        assert Thread.currentThread().isInterrupted();
        Thread.interrupted(); // clear for test cleanliness
    }
}
