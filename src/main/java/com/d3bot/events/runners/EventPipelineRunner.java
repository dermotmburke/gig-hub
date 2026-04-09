package com.d3bot.events.runners;

import com.d3bot.events.routes.EventRouteBuilder;
import org.apache.camel.ProducerTemplate;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EventPipelineRunner implements CommandLineRunner {

    private final ProducerTemplate producerTemplate;
    private final List<EventRouteBuilder> routes;

    public EventPipelineRunner(ProducerTemplate producerTemplate, List<EventRouteBuilder> routes) {
        this.producerTemplate = producerTemplate;
        this.routes = routes;
    }

    @Override
    public void run(String... args) {
        routes.forEach(route -> producerTemplate.sendBody("direct:" + route.getRouteId(), null));
    }
}
