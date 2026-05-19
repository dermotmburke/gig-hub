package com.d3bot.events.notifiers;

import com.d3bot.events.models.Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.env.MockEnvironment;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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

    // --- GET target ---

    @Test
    void getTargetSendsGetRequest() throws Exception {
        var env = new MockEnvironment()
                .withProperty("heartbeat.targets[0].url", "https://hc-ping.com/abc123");

        notifier(env).notify(List.of());

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(captor.capture(), any());
        assertThat(captor.getValue().method()).isEqualTo("GET");
        assertThat(captor.getValue().uri().toString()).isEqualTo("https://hc-ping.com/abc123");
    }

    @Test
    void getTargetSendsNoAuthorizationHeader() throws Exception {
        var env = new MockEnvironment()
                .withProperty("heartbeat.targets[0].url", "https://hc-ping.com/abc123");

        notifier(env).notify(List.of());

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(captor.capture(), any());
        assertThat(captor.getValue().headers().firstValue("Authorization")).isEmpty();
    }

    @Test
    void getTargetWithTokenIncludesBearerHeader() throws Exception {
        var env = new MockEnvironment()
                .withProperty("heartbeat.targets[0].url", "https://hc-ping.com/abc123")
                .withProperty("heartbeat.targets[0].token", "my-token");

        notifier(env).notify(List.of());

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(captor.capture(), any());
        assertThat(captor.getValue().method()).isEqualTo("GET");
        assertThat(captor.getValue().headers().firstValue("Authorization")).hasValue("Bearer my-token");
    }

    // --- POST + Bearer target ---

    @Test
    void postTargetSendsPostRequest() throws Exception {
        var env = new MockEnvironment()
                .withProperty("heartbeat.targets[0].url", "https://gatus.example.com/api/v1/endpoints/jobs_gig-hub/external?success=true")
                .withProperty("heartbeat.targets[0].method", "POST")
                .withProperty("heartbeat.targets[0].token", "secret-token");

        notifier(env).notify(List.of());

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(captor.capture(), any());
        assertThat(captor.getValue().method()).isEqualTo("POST");
    }

    @Test
    void postTargetIncludesBearerToken() throws Exception {
        var env = new MockEnvironment()
                .withProperty("heartbeat.targets[0].url", "https://gatus.example.com/api/v1/endpoints/jobs_gig-hub/external?success=true")
                .withProperty("heartbeat.targets[0].method", "POST")
                .withProperty("heartbeat.targets[0].token", "secret-token");

        notifier(env).notify(List.of());

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(captor.capture(), any());
        assertThat(captor.getValue().headers().firstValue("Authorization"))
                .hasValue("Bearer secret-token");
    }

    @Test
    void postTargetWithoutTokenSendsNoAuthorizationHeader() throws Exception {
        var env = new MockEnvironment()
                .withProperty("heartbeat.targets[0].url", "https://example.com/push")
                .withProperty("heartbeat.targets[0].method", "POST");

        notifier(env).notify(List.of());

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(captor.capture(), any());
        assertThat(captor.getValue().method()).isEqualTo("POST");
        assertThat(captor.getValue().headers().firstValue("Authorization")).isEmpty();
    }

    // --- Missing method defaults to GET ---

    @Test
    void missingMethodDefaultsToGet() throws Exception {
        var env = new MockEnvironment()
                .withProperty("heartbeat.targets[0].url", "https://hc-ping.com/abc123");
        // no method property set

        notifier(env).notify(List.of());

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(captor.capture(), any());
        assertThat(captor.getValue().method()).isEqualTo("GET");
    }

    // --- Mixed list ---

    @Test
    void mixedListPingsAllTargets() throws Exception {
        var env = new MockEnvironment()
                .withProperty("heartbeat.targets[0].url", "https://hc-ping.com/abc123")
                .withProperty("heartbeat.targets[1].url", "https://gatus.example.com/api/v1/endpoints/jobs_gig-hub/external?success=true")
                .withProperty("heartbeat.targets[1].method", "POST")
                .withProperty("heartbeat.targets[1].token", "secret-token");

        notifier(env).notify(List.of());

        verify(httpClient, times(2)).send(any(HttpRequest.class), any());
    }

    @Test
    void mixedListSendsCorrectMethodPerTarget() throws Exception {
        var env = new MockEnvironment()
                .withProperty("heartbeat.targets[0].url", "https://hc-ping.com/abc123")
                .withProperty("heartbeat.targets[1].url", "https://gatus.example.com/api/v1/endpoints/jobs_gig-hub/external?success=true")
                .withProperty("heartbeat.targets[1].method", "POST")
                .withProperty("heartbeat.targets[1].token", "secret-token");

        notifier(env).notify(List.of());

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient, times(2)).send(captor.capture(), any());
        List<HttpRequest> requests = captor.getAllValues();
        assertThat(requests.get(0).method()).isEqualTo("GET");
        assertThat(requests.get(1).method()).isEqualTo("POST");
        assertThat(requests.get(1).headers().firstValue("Authorization")).hasValue("Bearer secret-token");
    }

    // --- Resilience ---

    @Test
    void oneTargetFailingDoesNotStopOthers() throws Exception {
        var env = new MockEnvironment()
                .withProperty("heartbeat.targets[0].url", "https://hc-ping.com/abc123")
                .withProperty("heartbeat.targets[1].url", "https://monitor-b.example.com/ping");

        doThrow(new IOException("connection refused"))
                .doReturn(mock(HttpResponse.class))
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

        assertThat(Thread.currentThread().isInterrupted()).isTrue();
        Thread.interrupted(); // clear for test cleanliness
    }

    @Test
    void non2xxResponseLogsWarnButDoesNotThrow() throws Exception {
        var env = new MockEnvironment()
                .withProperty("heartbeat.targets[0].url", "https://hc-ping.com/abc123");

        HttpResponse<Void> badResponse = mock(HttpResponse.class);
        when(badResponse.statusCode()).thenReturn(503);
        doReturn(badResponse).when(httpClient).send(any(), any());

        // should not throw
        notifier(env).notify(List.of());

        verify(httpClient).send(any(HttpRequest.class), any());
    }

    // --- Events payload ---

    @Test
    void notifyPingsWithNoEvents() throws Exception {
        var env = new MockEnvironment()
                .withProperty("heartbeat.targets[0].url", "https://hc-ping.com/abc123");

        notifier(env).notify(List.of());

        verify(httpClient, atLeastOnce()).send(any(HttpRequest.class), any());
    }

    @Test
    void notifyPingsWithEvents() throws Exception {
        var env = new MockEnvironment()
                .withProperty("heartbeat.targets[0].url", "https://hc-ping.com/abc123");

        notifier(env).notify(List.of(
                new Event("The Cure", "O2 Arena", LocalDateTime.of(2026, 5, 10, 19, 0), "https://example.com")));

        verify(httpClient, atLeastOnce()).send(any(HttpRequest.class), any());
    }
}
