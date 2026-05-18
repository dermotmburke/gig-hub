package com.d3bot.events.utilities;

import com.d3bot.events.pipelines.BanquetEventPipeline;
import com.d3bot.events.pipelines.EventPipeline;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RouteIdBuilderTest {

    static class MultiWordExampleEventPipeline extends EventPipeline {
        MultiWordExampleEventPipeline() {
            super(() -> "", raw -> List.of(), List.of(), Optional.empty());
        }
    }

    @Test
    void stripsEventPipelineSuffix() {
        assertEquals("banquet-pipeline", RouteIdBuilder.build(BanquetEventPipeline.class));
    }

    @Test
    void convertsMultiWordClassNameToKebabCase() {
        assertEquals("multi-word-example-pipeline", RouteIdBuilder.build(MultiWordExampleEventPipeline.class));
    }
}
