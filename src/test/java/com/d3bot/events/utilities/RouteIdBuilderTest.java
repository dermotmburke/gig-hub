package com.d3bot.events.utilities;

import com.d3bot.events.routes.BanquetEventRouteBuilder;
import com.d3bot.events.routes.EventRouteBuilder;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RouteIdBuilderTest {

    static class MultiWordExampleEventRouteBuilder extends EventRouteBuilder {
        MultiWordExampleEventRouteBuilder() {
            super(() -> "", raw -> List.of(), List.of(), Optional.empty());
        }
    }

    @Test
    void stripsEventRouteBuilderSuffix() {
        assertEquals("banquet-pipeline", RouteIdBuilder.build(BanquetEventRouteBuilder.class));
    }

    @Test
    void convertsMultiWordClassNameToKebabCase() {
        assertEquals("multi-word-example-pipeline", RouteIdBuilder.build(MultiWordExampleEventRouteBuilder.class));
    }
}
