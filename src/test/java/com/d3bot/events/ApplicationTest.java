package com.d3bot.events;

import com.d3bot.events.models.Event;
import com.d3bot.events.notifiers.LoggingEventNotifier;
import com.d3bot.events.runners.EventPipelineRunner;
import com.d3bot.events.utilities.UrlFetcher;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApplicationTest {

    @MockBean
    UrlFetcher urlFetcher;

    @SpyBean
    LoggingEventNotifier notifier;

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    EventPipelineRunner runner;

    @Test
    void contextLoads() {
    }

    @Test
    void healthEndpointReturnsUp() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health", String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("\"status\":\"UP\""));
    }

    @Test
    @SuppressWarnings("unchecked")
    void pipelineRunsAndNotifiesEvents() throws Exception {
        String html = Files.readString(new ClassPathResource("events.html").getFile().toPath());
        when(urlFetcher.fetch(anyString())).thenReturn(html);

        runner.run();

        ArgumentCaptor<List<Event>> captor = ArgumentCaptor.forClass(List.class);
        verify(notifier).notify(captor.capture());
        assertFalse(captor.getValue().isEmpty());
    }
}
