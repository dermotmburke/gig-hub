package com.d3bot.events.jobs;

import com.d3bot.events.pipelines.EventPipeline;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.*;

class EventScrapeJobTest {

    @Test
    void runCallsRunOnEachPipeline() {
        EventPipeline pipeline1 = mock(EventPipeline.class);
        EventPipeline pipeline2 = mock(EventPipeline.class);
        EventScrapeJob job = new EventScrapeJob(List.of(pipeline1, pipeline2));

        job.run();

        verify(pipeline1).run();
        verify(pipeline2).run();
    }
}
