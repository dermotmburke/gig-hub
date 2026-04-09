package com.d3bot.events.routes;

import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EventSchedulerRoute extends RouteBuilder {

    static final String ROUTE_ID = "pipeline-scheduler";

    private final ProducerTemplate producerTemplate;
    private final List<EventRouteBuilder> pipelineRoutes;
    private final long intervalMs;

    public EventSchedulerRoute(
            ProducerTemplate producerTemplate,
            List<EventRouteBuilder> pipelineRoutes,
            @Value("${scraper.interval-ms:3600000}") long intervalMs) {
        this.producerTemplate = producerTemplate;
        this.pipelineRoutes = pipelineRoutes;
        this.intervalMs = intervalMs;
    }

    @Override
    public void configure() {
        from("timer://pipelines?period=" + intervalMs + "&delay=0")
                .routeId(ROUTE_ID)
                .process(exchange ->
                        pipelineRoutes.forEach(route ->
                                producerTemplate.sendBody("direct:" + route.getRouteId(), null)));
    }
}
