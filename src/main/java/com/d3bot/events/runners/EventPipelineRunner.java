package com.d3bot.events.runners;

import com.d3bot.events.pipelines.EventPipeline;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EventPipelineRunner implements CommandLineRunner {

    private final List<EventPipeline> pipelines;

    public EventPipelineRunner(List<EventPipeline> pipelines) {
        this.pipelines = pipelines;
    }

    @Override
    public void run(String... args) {
        pipelines.forEach(EventPipeline::run);
    }
}
