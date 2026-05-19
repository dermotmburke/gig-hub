package com.d3bot.events;

import com.d3bot.events.deduplicators.EventDeduplicator;
import com.d3bot.events.models.Event;
import com.d3bot.events.notifiers.LoggingEventNotifier;
import com.d3bot.events.runners.EventPipelineRunner;
import com.d3bot.events.utilities.UrlFetcher;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ClassPathResource;

import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "spring.main.banner-mode=off")
class ApplicationTest {

    @MockBean
    UrlFetcher urlFetcher;

    @MockBean
    LoggingEventNotifier notifier;

    @MockBean
    EventDeduplicator deduplicator;

    @Autowired
    EventPipelineRunner runner;

    @Test
    void contextLoads() {
    }

    @Test
    @SuppressWarnings("unchecked")
    void pipelineDeduplicatesAndNotifiesEvents() throws Exception {
        String html = Files.readString(new ClassPathResource("events.html").getFile().toPath());
        when(urlFetcher.fetch(anyString())).thenReturn(html);
        when(deduplicator.filter(anyList())).thenAnswer(inv -> inv.getArgument(0));

        runner.run();

        ArgumentCaptor<List<Event>> captor = ArgumentCaptor.forClass(List.class);
        verify(notifier).notify(captor.capture());
        assertFalse(captor.getValue().isEmpty());
        verify(deduplicator).filter(captor.getValue());
        verify(deduplicator).markSent(captor.getValue());
    }
}
