package com.d3bot.events.runners;

import com.d3bot.events.notifiers.HeartbeatReporter;
import com.d3bot.events.pipelines.EventPipeline;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class EventPipelineRunner implements CommandLineRunner {

    private final List<EventPipeline> pipelines;
    private final Optional<HeartbeatReporter> heartbeat;

    public EventPipelineRunner(List<EventPipeline> pipelines, Optional<HeartbeatReporter> heartbeat) {
        this.pipelines = pipelines;
        this.heartbeat = heartbeat;
    }

    @Override
    public void run(String... args) {
        List<String> failed = new ArrayList<>();
        for (EventPipeline pipeline : pipelines) {
            if (!pipeline.run()) {
                failed.add(pipeline.getPipelineId());
            }
        }
        heartbeat.ifPresent(h -> h.report(failed));
    }
}
