package com.d3bot.events.routes;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.impl.DefaultCamelContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.*;

class EventSchedulerRouteTest {

    private final ProducerTemplate mockTemplate = mock(ProducerTemplate.class);
    private CamelContext context;

    @AfterEach
    void tearDown() throws Exception {
        if (context != null) {
            context.stop();
        }
    }

    @Test
    void timerTickDispatchesToAllPipelineRoutes() throws Exception {
        EventRouteBuilder route1 = mock(EventRouteBuilder.class);
        EventRouteBuilder route2 = mock(EventRouteBuilder.class);
        when(route1.getRouteId()).thenReturn("route-1");
        when(route2.getRouteId()).thenReturn("route-2");

        context = new DefaultCamelContext();
        context.addRoutes(new EventSchedulerRoute(mockTemplate, List.of(route1, route2), 3600000));
        AdviceWith.adviceWith(context, EventSchedulerRoute.ROUTE_ID, advice ->
                advice.replaceFromWith("direct:test-trigger"));
        context.start();

        try (ProducerTemplate trigger = context.createProducerTemplate()) {
            trigger.sendBody("direct:test-trigger", null);
        }

        verify(mockTemplate).sendBody("direct:route-1", null);
        verify(mockTemplate).sendBody("direct:route-2", null);
    }
}
