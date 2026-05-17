package com.d3bot.events.runners;

import com.d3bot.events.pipelines.EventPipeline;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
public class EventPipelineRunner implements CommandLineRunner {

    private final List<EventPipeline> pipelines;
    private final long intervalMs;

    public EventPipelineRunner(
            List<EventPipeline> pipelines,
            @Value("${runner.interval-ms:3600000}") long intervalMs) {
        this.pipelines = pipelines;
        this.intervalMs = intervalMs;
    }

    @Override
    public void run(String... args) {
        Executors.newSingleThreadScheduledExecutor()
                .scheduleAtFixedRate(this::runAll, 0, intervalMs, TimeUnit.MILLISECONDS);
    }

    void runAll() {
        pipelines.forEach(EventPipeline::run);
    }
}
