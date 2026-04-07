package com.d3bot.events.notifiers;

import com.d3bot.events.models.Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.List;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SlackEventNotifierTest {

    private final HttpClient httpClient = mock(HttpClient.class);
    private final SlackEventNotifier notifier = new SlackEventNotifier(httpClient, "https://hooks.slack.com/test");

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws Exception {
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        doReturn(200).when(mockResponse).statusCode();
        doReturn(mockResponse).when(httpClient).send(any(HttpRequest.class), any());
    }

    @Test
    void notifySendsPostRequestToWebhook() throws Exception {
        notifier.notify(List.of(new Event("The Cure", "O2 Arena", LocalDateTime.of(2026, 5, 10, 19, 0), "https://banquetrecords.com/the-cure")));

        verify(httpClient).send(any(HttpRequest.class), any());
    }

    @Test
    void buildPayloadContainsArtistAndDate() {
        List<Event> events = List.of(
                new Event("The Cure", "O2 Arena", LocalDateTime.of(2026, 5, 10, 19, 0), "https://banquetrecords.com/the-cure"),
                new Event("Radiohead", "Wembley", LocalDateTime.of(2026, 5, 11, 20, 0), "https://banquetrecords.com/radiohead")
        );

        String payload = notifier.buildPayload(events);

        assertTrue(payload.contains("https://banquetrecords.com/the-cure|The Cure"));
        assertTrue(payload.contains("Sunday 10 May"));
        assertTrue(payload.contains("Radiohead"));
        assertTrue(payload.contains("2 upcoming events"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void notifyLogsErrorOnNon200Response() throws Exception {
        HttpResponse<String> errorResponse = mock(HttpResponse.class);
        doReturn(500).when(errorResponse).statusCode();
        doReturn("error").when(errorResponse).body();
        doReturn(errorResponse).when(httpClient).send(any(HttpRequest.class), any());

        notifier.notify(List.of(new Event("The Cure", "O2 Arena", LocalDateTime.of(2026, 5, 10, 19, 0), "https://example.com")));
    }

    @Test
    void notifyHandlesIOException() throws Exception {
        doThrow(new IOException("connection refused")).when(httpClient).send(any(), any());

        notifier.notify(List.of(new Event("The Cure", "O2 Arena", LocalDateTime.of(2026, 5, 10, 19, 0), "https://example.com")));
    }

    @Test
    void buildPayloadEscapesSpecialCharacters() {
        var event = new Event("Artist \"quoted\"", "Venue\\slash", LocalDateTime.of(2026, 5, 10, 19, 0), "/relative");
        String payload = notifier.buildPayload(List.of(event));
        assertTrue(payload.contains("\\\""));
    }

    @Test
    void notifyDoesNothingForEmptyList() throws Exception {
        notifier.notify(List.of());

        verify(httpClient, org.mockito.Mockito.never()).send(any(), any());
    }
}
