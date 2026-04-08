package com.d3bot.events.runners;

import com.d3bot.events.pipelines.EventPipeline;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.*;

class EventPipelineRunnerTest {

    @Test
    void runCallsRunOnEachPipeline() {
        EventPipeline pipeline1 = mock(EventPipeline.class);
        EventPipeline pipeline2 = mock(EventPipeline.class);
        EventPipelineRunner job = new EventPipelineRunner(List.of(pipeline1, pipeline2));

        job.run();

        verify(pipeline1).run();
        verify(pipeline2).run();
    }
}
