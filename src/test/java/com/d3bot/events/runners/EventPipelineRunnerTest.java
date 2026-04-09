package com.d3bot.events.runners;

import com.d3bot.events.routes.EventRouteBuilder;
import org.apache.camel.ProducerTemplate;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.*;

class EventPipelineRunnerTest {

    @Test
    void runTriggersAllRoutes() {
        ProducerTemplate template = mock(ProducerTemplate.class);
        EventRouteBuilder route1 = mock(EventRouteBuilder.class);
        EventRouteBuilder route2 = mock(EventRouteBuilder.class);
        when(route1.getRouteId()).thenReturn("route-1");
        when(route2.getRouteId()).thenReturn("route-2");

        EventPipelineRunner runner = new EventPipelineRunner(template, List.of(route1, route2));
        runner.run();

        verify(template).sendBody("direct:route-1", null);
        verify(template).sendBody("direct:route-2", null);
    }
}
