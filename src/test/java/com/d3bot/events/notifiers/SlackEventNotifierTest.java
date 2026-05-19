package com.d3bot.events.notifiers;

import com.d3bot.events.models.Event;
import com.d3bot.events.utilities.GigHubCalendarUrlBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class SlackEventNotifierTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Event CURE = new Event(
            "The Cure", "O2 Arena", LocalDateTime.of(2026, 5, 10, 19, 0), "https://banquetrecords.com/the-cure");

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
    void notifySendsJsonPostRequestToWebhook() throws Exception {
        notifier.notify(List.of(CURE));

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(captor.capture(), any());

        HttpRequest request = captor.getValue();
        assertEquals("POST", request.method());
        assertEquals(URI.create("https://hooks.slack.com/test"), request.uri());
        assertEquals(Optional.of("application/json"), request.headers().firstValue("Content-Type"));
        assertTrue(request.bodyPublisher().isPresent());
    }

    @Test
    void buildPayloadCreatesValidJsonContainingArtistDateAndCount() throws Exception {
        List<Event> events = List.of(
                CURE,
                new Event("Radiohead", "Wembley", LocalDateTime.of(2026, 5, 11, 20, 0), "https://banquetrecords.com/radiohead")
        );

        JsonNode payload = OBJECT_MAPPER.readTree(notifier.buildPayload(events));
        String text = payload.path("text").asText();

        assertEquals(1, payload.size());
        assertTrue(text.contains("https://banquetrecords.com/the-cure|The Cure"));
        assertTrue(text.contains("Sunday 10 May"));
        assertTrue(text.contains("Radiohead"));
        assertTrue(text.startsWith("*2 upcoming events*"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void notifyThrowsOnNon200Response() throws Exception {
        HttpResponse<String> errorResponse = mock(HttpResponse.class);
        doReturn(500).when(errorResponse).statusCode();
        doReturn("error").when(errorResponse).body();
        doReturn(errorResponse).when(httpClient).send(any(HttpRequest.class), any());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> notifier.notify(List.of(CURE)));

        assertTrue(exception.getMessage().contains("500"));
        assertTrue(exception.getMessage().contains("error"));
    }

    @Test
    void notifyHandlesIOException() throws Exception {
        doThrow(new IOException("connection refused")).when(httpClient).send(any(), any());

        notifier.notify(List.of(CURE));

        assertTrue(Thread.currentThread().isInterrupted());
        Thread.interrupted(); // clear for test cleanliness
    }

    @Test
    void buildPayloadEscapesSpecialCharactersAndRemainsValidJson() throws Exception {
        var event = new Event("Artist \"quoted\"", "Venue\\slash", LocalDateTime.of(2026, 5, 10, 19, 0), "/relative");

        JsonNode payload = OBJECT_MAPPER.readTree(notifier.buildPayload(List.of(event)));
        String text = payload.path("text").asText();

        assertTrue(text.contains("Artist \"quoted\""));
        assertTrue(text.contains("Venue\\slash"));
    }

    @Test
    void notifyDoesNothingForEmptyList() throws Exception {
        notifier.notify(List.of());

        verify(httpClient, never()).send(any(), any());
    }

    @Test
    void buildPayloadDoesNotContainSaveLinkWhenBuilderAbsent() throws Exception {
        String text = OBJECT_MAPPER.readTree(notifier.buildPayload(List.of(CURE))).path("text").asText();

        assertFalse(text.contains("/save?"), "Save link should be absent when builder is not present");
    }

    @Test
    void buildPayloadContainsSaveLinkWhenBuilderPresent() throws Exception {
        var notifierWithSaver = new SlackEventNotifier(httpClient, "https://hooks.slack.com/test",
                Optional.of(new GigHubCalendarUrlBuilder("http://localhost:3000")));

        String text = OBJECT_MAPPER.readTree(notifierWithSaver.buildPayload(List.of(CURE))).path("text").asText();

        assertTrue(text.contains("http://localhost:3000/save?"), "Save link should be present");
        assertTrue(text.contains("artist=The+Cure"), "Artist should be URL-encoded");
        assertTrue(text.contains("location=O2+Arena"), "Location should be URL-encoded");
        assertTrue(text.contains("date=2026-05-10T19:00:00"), "Date should be ISO format");
    }

    @Test
    void buildPayloadUrlEncodesSpecialCharsInTicketUrl() throws Exception {
        var notifierWithSaver = new SlackEventNotifier(httpClient, "https://hooks.slack.com/test",
                Optional.of(new GigHubCalendarUrlBuilder("http://localhost:3000")));
        var event = new Event("Band", "Venue", LocalDateTime.of(2026, 5, 10, 19, 0), "https://example.com/event?id=123&ref=slack");

        String text = OBJECT_MAPPER.readTree(notifierWithSaver.buildPayload(List.of(event))).path("text").asText();

        assertTrue(text.contains("url=https%3A%2F%2Fexample.com%2Fevent%3Fid%3D123%26ref%3Dslack"),
                "Ticket URL with & and ? should be fully encoded");
    }
}
