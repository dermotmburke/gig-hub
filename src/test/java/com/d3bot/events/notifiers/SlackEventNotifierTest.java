package com.d3bot.events.notifiers;

import com.d3bot.events.models.Event;
import com.d3bot.events.utilities.GigHubCalendarUrlBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SlackEventNotifierTest {

    private final HttpClient httpClient = mock(HttpClient.class);
    private final SlackEventNotifier notifier = new SlackEventNotifier(httpClient, "https://hooks.slack.com/test", Optional.empty());

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
    void notifyThrowsOnNon200Response() throws Exception {
        HttpResponse<String> errorResponse = mock(HttpResponse.class);
        doReturn(500).when(errorResponse).statusCode();
        doReturn("error").when(errorResponse).body();
        doReturn(errorResponse).when(httpClient).send(any(HttpRequest.class), any());

        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () ->
                notifier.notify(List.of(new Event("The Cure", "O2 Arena", LocalDateTime.of(2026, 5, 10, 19, 0), "https://example.com"))));
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

    @Test
    void buildPayloadDoesNotContainSaveLinkWhenBuilderAbsent() {
        var event = new Event("The Cure", "O2 Arena", LocalDateTime.of(2026, 5, 10, 19, 0), "https://example.com");
        String payload = notifier.buildPayload(List.of(event));

        assertFalse(payload.contains("/save?"), "Save link should be absent when builder is not present");
    }

    @Test
    void buildPayloadContainsSaveLinkWhenBuilderPresent() {
        var notifierWithSaver = new SlackEventNotifier(httpClient, "https://hooks.slack.com/test",
                Optional.of(new GigHubCalendarUrlBuilder("http://localhost:3000")));
        var event = new Event("The Cure", "O2 Arena", LocalDateTime.of(2026, 5, 10, 19, 0), "https://example.com");

        String payload = notifierWithSaver.buildPayload(List.of(event));

        assertTrue(payload.contains("http://localhost:3000/save?"), "Save link should be present");
        assertTrue(payload.contains("artist=The+Cure"), "Artist should be URL-encoded");
        assertTrue(payload.contains("location=O2+Arena"), "Location should be URL-encoded");
        assertTrue(payload.contains("date=2026-05-10T19:00:00"), "Date should be ISO format");
    }

    @Test
    void buildPayloadUrlEncodesSpecialCharsInTicketUrl() {
        var notifierWithSaver = new SlackEventNotifier(httpClient, "https://hooks.slack.com/test",
                Optional.of(new GigHubCalendarUrlBuilder("http://localhost:3000")));
        var event = new Event("Band", "Venue", LocalDateTime.of(2026, 5, 10, 19, 0), "https://example.com/event?id=123&ref=slack");

        String payload = notifierWithSaver.buildPayload(List.of(event));

        assertTrue(payload.contains("url=https%3A%2F%2Fexample.com%2Fevent%3Fid%3D123%26ref%3Dslack"),
                "Ticket URL with & and ? should be fully encoded");
    }
}
