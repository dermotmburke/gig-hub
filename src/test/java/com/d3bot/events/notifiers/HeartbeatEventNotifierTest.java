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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HeartbeatEventNotifierTest {

    private final HttpClient httpClient = mock(HttpClient.class);

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws Exception {
        HttpResponse<Void> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        doReturn(mockResponse).when(httpClient).send(any(HttpRequest.class), any());
    }

    private HeartbeatEventNotifier notifier(MockEnvironment env) {
        return new HeartbeatEventNotifier(httpClient, env);
    }

    @Test
    void getTargetSendsGetRequestToConfiguredUrl() throws Exception {
        var env = new MockEnvironment()
                .withProperty("heartbeat.targets[0].url", "https://hc-ping.com/abc123");

        notifier(env).notify(List.of());

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(captor.capture(), any());
        HttpRequest request = captor.getValue();
        assertEquals("GET", request.method());
        assertEquals(URI.create("https://hc-ping.com/abc123"), request.uri());
    }

    @Test
    void getTargetSendsNoAuthorizationHeaderWhenTokenIsAbsent() throws Exception {
        var env = new MockEnvironment()
                .withProperty("heartbeat.targets[0].url", "https://hc-ping.com/abc123");

        notifier(env).notify(List.of());

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(captor.capture(), any());
        assertTrue(captor.getValue().headers().firstValue("Authorization").isEmpty());
    }

    @Test
    void getTargetWithTokenIncludesBearerHeader() throws Exception {
        var env = new MockEnvironment()
                .withProperty("heartbeat.targets[0].url", "https://hc-ping.com/abc123")
                .withProperty("heartbeat.targets[0].token", "my-token");

        notifier(env).notify(List.of());

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(captor.capture(), any());
        HttpRequest request = captor.getValue();
        assertEquals("GET", request.method());
        assertEquals("Bearer my-token", request.headers().firstValue("Authorization").orElseThrow());
    }

    @Test
    void postTargetSendsPostRequestWithBearerToken() throws Exception {
        var env = new MockEnvironment()
                .withProperty("heartbeat.targets[0].url", "https://gatus.example.com/api/v1/endpoints/jobs_gig-hub/external?success=true")
                .withProperty("heartbeat.targets[0].method", "POST")
                .withProperty("heartbeat.targets[0].token", "secret-token");

        notifier(env).notify(List.of());

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(captor.capture(), any());
        HttpRequest request = captor.getValue();
        assertEquals("POST", request.method());
        assertEquals(URI.create("https://gatus.example.com/api/v1/endpoints/jobs_gig-hub/external?success=true"), request.uri());
        assertEquals("Bearer secret-token", request.headers().firstValue("Authorization").orElseThrow());
    }

    @Test
    void postTargetWithoutTokenSendsNoAuthorizationHeader() throws Exception {
        var env = new MockEnvironment()
                .withProperty("heartbeat.targets[0].url", "https://example.com/push")
                .withProperty("heartbeat.targets[0].method", "POST");

        notifier(env).notify(List.of());

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(captor.capture(), any());
        assertEquals("POST", captor.getValue().method());
        assertTrue(captor.getValue().headers().firstValue("Authorization").isEmpty());
    }

    @Test
    void missingMethodDefaultsToGet() throws Exception {
        var env = new MockEnvironment()
                .withProperty("heartbeat.targets[0].url", "https://hc-ping.com/abc123");

        notifier(env).notify(List.of());

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(captor.capture(), any());
        assertEquals("GET", captor.getValue().method());
    }

    @Test
    void mixedListPingsAllTargetsWithCorrectMethodsAndHeaders() throws Exception {
        var env = new MockEnvironment()
                .withProperty("heartbeat.targets[0].url", "https://hc-ping.com/abc123")
                .withProperty("heartbeat.targets[1].url", "https://gatus.example.com/api/v1/endpoints/jobs_gig-hub/external?success=true")
                .withProperty("heartbeat.targets[1].method", "POST")
                .withProperty("heartbeat.targets[1].token", "secret-token");

        notifier(env).notify(List.of());

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient, times(2)).send(captor.capture(), any());
        List<HttpRequest> requests = captor.getAllValues();
        assertEquals(List.of(
                URI.create("https://hc-ping.com/abc123"),
                URI.create("https://gatus.example.com/api/v1/endpoints/jobs_gig-hub/external?success=true")),
                requests.stream().map(HttpRequest::uri).toList());
        assertEquals(List.of("GET", "POST"), requests.stream().map(HttpRequest::method).toList());
        assertTrue(requests.get(0).headers().firstValue("Authorization").isEmpty());
        assertEquals("Bearer secret-token", requests.get(1).headers().firstValue("Authorization").orElseThrow());
    }

    @Test
    void oneTargetFailingDoesNotStopOthers() throws Exception {
        var env = new MockEnvironment()
                .withProperty("heartbeat.targets[0].url", "https://hc-ping.com/abc123")
                .withProperty("heartbeat.targets[1].url", "https://monitor-b.example.com/ping");

        HttpResponse<Void> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        doThrow(new IOException("connection refused"))
                .doReturn(response)
                .when(httpClient).send(any(), any());

        notifier(env).notify(List.of());

        verify(httpClient, times(2)).send(any(HttpRequest.class), any());
    }

    @Test
    void interruptedExceptionSetsInterruptFlag() throws Exception {
        var env = new MockEnvironment()
                .withProperty("heartbeat.targets[0].url", "https://hc-ping.com/abc123");

        doThrow(new InterruptedException()).when(httpClient).send(any(), any());

        notifier(env).notify(List.of());

        assertTrue(Thread.currentThread().isInterrupted());
        Thread.interrupted(); // clear for test cleanliness
    }

    @Test
    void non2xxResponseDoesNotThrow() throws Exception {
        var env = new MockEnvironment()
                .withProperty("heartbeat.targets[0].url", "https://hc-ping.com/abc123");

        HttpResponse<Void> badResponse = mock(HttpResponse.class);
        when(badResponse.statusCode()).thenReturn(503);
        doReturn(badResponse).when(httpClient).send(any(), any());

        assertDoesNotThrow(() -> notifier(env).notify(List.of()));
        verify(httpClient).send(any(HttpRequest.class), any());
    }

    @Test
    void notifyPingsWithEvents() throws Exception {
        var env = new MockEnvironment()
                .withProperty("heartbeat.targets[0].url", "https://hc-ping.com/abc123");

        notifier(env).notify(List.of(
                new Event("The Cure", "O2 Arena", LocalDateTime.of(2026, 5, 10, 19, 0), "https://example.com")));

        verify(httpClient).send(any(HttpRequest.class), any());
    }

    @Test
    void ignoresBlankOrMissingTargetUrls() throws Exception {
        var env = new MockEnvironment()
                .withProperty("heartbeat.targets[0].url", "")
                .withProperty("heartbeat.targets[1].method", "POST");

        notifier(env).notify(List.of());

        verify(httpClient, never()).send(any(HttpRequest.class), any());
    }
}
