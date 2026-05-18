package com.d3bot.events.config;

import com.d3bot.events.deduplicators.EventDeduplicator;
import com.d3bot.events.extractors.TicketmasterEventExtractor;
import com.d3bot.events.notifiers.EventNotifier;
import com.d3bot.events.pipelines.TicketmasterVenueEventPipeline;
import com.d3bot.events.utilities.UrlFetcher;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class TicketmasterPipelineFactoryTest {

    private final TicketmasterPipelineFactory factory = new TicketmasterPipelineFactory(
            mock(UrlFetcher.class),
            mock(TicketmasterEventExtractor.class),
            List.of(mock(EventNotifier.class)),
            Optional.of(mock(EventDeduplicator.class)));

    @Test
    void createReturnsPipelineWithCorrectId() {
        TicketmasterVenueEventPipeline pipeline = factory.create("brixton-academy", "KovZ91777af", "test-key");
        assertEquals("brixton-academy-pipeline", pipeline.getPipelineId());
    }

    @Test
    void createProducesDistinctPipelinesForDifferentVenues() {
        TicketmasterVenueEventPipeline a = factory.create("venue-a", "id-a", "test-key");
        TicketmasterVenueEventPipeline b = factory.create("venue-b", "id-b", "test-key");
        assertEquals("venue-a-pipeline", a.getPipelineId());
        assertEquals("venue-b-pipeline", b.getPipelineId());
    }
}
