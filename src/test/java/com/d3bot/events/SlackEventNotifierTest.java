package com.d3bot.events;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SlackEventNotifierTest {

    private final HttpClient httpClient = mock(HttpClient.class);
    private final SlackEventNotifier notifier = new SlackEventNotifier(httpClient, "https://hooks.slack.com/test", "#test-events");

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws Exception {
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        doReturn(200).when(mockResponse).statusCode();
        doReturn(mockResponse).when(httpClient).send(any(HttpRequest.class), any());
    }

    @Test
    void notifySendsPostRequestToWebhook() throws Exception {
        notifier.notify(List.of(new Event("The Cure", "O2 Arena", "Friday 10th May", "/the-cure")));

        verify(httpClient).send(any(HttpRequest.class), any());
    }

    @Test
    void buildPayloadContainsArtistAndDate() {
        List<Event> events = List.of(
                new Event("The Cure", "O2 Arena", "Friday 10th May", "/the-cure"),
                new Event("Radiohead", "Wembley", "Saturday 11th May", "/radiohead")
        );

        String payload = notifier.buildPayload(events);

        assertTrue(payload.contains("The Cure"));
        assertTrue(payload.contains("Friday 10th May"));
        assertTrue(payload.contains("Radiohead"));
        assertTrue(payload.contains("2 upcoming events"));
        assertTrue(payload.contains("#test-events"));
    }

    @Test
    void notifyDoesNothingForEmptyList() throws Exception {
        notifier.notify(List.of());

        verify(httpClient, org.mockito.Mockito.never()).send(any(), any());
    }
}
