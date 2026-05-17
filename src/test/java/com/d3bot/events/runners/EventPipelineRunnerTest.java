package com.d3bot.events.runners;

import com.d3bot.events.pipelines.EventPipeline;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

class EventPipelineRunnerTest {

    @Test
    void runCallsEachPipeline() throws Exception {
        EventPipeline p1 = mock(EventPipeline.class);
        EventPipeline p2 = mock(EventPipeline.class);
        new EventPipelineRunner(List.of(p1, p2)).run();
        verify(p1).run();
        verify(p2).run();
    }

    @Test
    void runHandlesEmptyPipelineList() {
        assertDoesNotThrow(() -> new EventPipelineRunner(List.of()).run());
    }
}
