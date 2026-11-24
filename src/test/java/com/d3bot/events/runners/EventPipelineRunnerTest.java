package com.d3bot.events.runners;

import com.d3bot.events.notifiers.HeartbeatReporter;
import com.d3bot.events.pipelines.EventPipeline;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

class EventPipelineRunnerTest {

    private final HeartbeatReporter heartbeat = mock(HeartbeatReporter.class);

    private static EventPipeline pipeline(String id, boolean succeeds) {
        EventPipeline p = mock(EventPipeline.class);
        when(p.getPipelineId()).thenReturn(id);
        when(p.run()).thenReturn(succeeds);
        return p;
    }

    @Test
    void runCallsEachPipeline() {
        EventPipeline p1 = pipeline("a-pipeline", true);
        EventPipeline p2 = pipeline("b-pipeline", true);
        new EventPipelineRunner(List.of(p1, p2), Optional.empty()).run();
        verify(p1).run();
        verify(p2).run();
    }

    @Test
    void reportsSuccessOnceWhenAllPipelinesSucceed() {
        new EventPipelineRunner(List.of(pipeline("a-pipeline", true), pipeline("b-pipeline", true)),
                Optional.of(heartbeat)).run();
        verify(heartbeat, times(1)).report(List.of());
    }

    @Test
    void reportsFailedPipelineIds() {
        EventPipeline ok = pipeline("a-pipeline", true);
        EventPipeline bad = pipeline("banquet-pipeline", false);
        new EventPipelineRunner(List.of(bad, ok), Optional.of(heartbeat)).run();
        verify(ok).run();
        verify(heartbeat, times(1)).report(List.of("banquet-pipeline"));
    }

    @Test
    void runHandlesEmptyPipelineList() {
        assertDoesNotThrow(() -> new EventPipelineRunner(List.of(), Optional.empty()).run());
    }
}
