package com.d3bot.events.utilities;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Flow;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FlareSolverrClientTest {

    private static final String ENDPOINT = "http://flaresolverr.gig-hub.svc:8191/v1";
    private static final String TARGET = "https://www.banquetrecords.com/events?w=1000";

    private final HttpClient httpClient = mock(HttpClient.class);
    private final FlareSolverrClient client = new FlareSolverrClient(httpClient, ENDPOINT);

    @SuppressWarnings("unchecked")
    private void respond(int status, String body) throws Exception {
        HttpResponse<String> response = mock(HttpResponse.class);
        doReturn(status).when(response).statusCode();
        doReturn(body).when(response).body();
        doReturn(response).when(httpClient).send(any(HttpRequest.class), any());
    }

    @Test
    void returnsRenderedHtmlAndPostsRequestGetCommand() throws Exception {
        respond(200, "{\"status\":\"ok\",\"message\":\"\",\"solution\":{\"status\":200,\"response\":\"<html>gigs</html>\"}}");

        assertEquals("<html>gigs</html>", client.fetch(TARGET));

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(captor.capture(), any());
        HttpRequest request = captor.getValue();
        assertEquals("POST", request.method());
        assertEquals(URI.create(ENDPOINT), request.uri());
        assertEquals("application/json", request.headers().firstValue("Content-Type").orElseThrow());

        JsonNode sent = new ObjectMapper().readTree(bodyOf(request));
        assertEquals("request.get", sent.get("cmd").asText());
        assertEquals(TARGET, sent.get("url").asText());
        assertEquals(FlareSolverrClient.MAX_TIMEOUT_MS, sent.get("maxTimeout").asInt());
    }

    @Test
    void throwsWhenFlareSolverrReturnsNon200() throws Exception {
        respond(500, "boom");
        IOException e = assertThrows(IOException.class, () -> client.fetch(TARGET));
        assertTrue(e.getMessage().contains("500"));
    }

    @Test
    void throwsWhenFlareSolverrReportsError() throws Exception {
        respond(200, "{\"status\":\"error\",\"message\":\"Timeout after 120.0 seconds.\"}");
        IOException e = assertThrows(IOException.class, () -> client.fetch(TARGET));
        assertTrue(e.getMessage().contains("Timeout after 120.0 seconds."));
    }

    @Test
    void throwsWhenTargetSiteReturnsNon200() throws Exception {
        respond(200, "{\"status\":\"ok\",\"message\":\"\",\"solution\":{\"status\":403,\"response\":\"blocked\"}}");
        IOException e = assertThrows(IOException.class, () -> client.fetch(TARGET));
        assertTrue(e.getMessage().contains("403"));
    }

    private static String bodyOf(HttpRequest request) {
        StringBuilder sb = new StringBuilder();
        request.bodyPublisher().orElseThrow().subscribe(new Flow.Subscriber<>() {
            public void onSubscribe(Flow.Subscription s) { s.request(Long.MAX_VALUE); }
            public void onNext(ByteBuffer item) { sb.append(StandardCharsets.UTF_8.decode(item)); }
            public void onError(Throwable t) { throw new RuntimeException(t); }
            public void onComplete() { }
        });
        return sb.toString();
    }
}
