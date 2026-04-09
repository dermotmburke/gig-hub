package com.d3bot.events.routes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RouteIdDeriverTest {

    @Test
    void stripsEventRouteBuilderSuffix() {
        assertEquals("banquet-pipeline", RouteIdDeriver.deriveRouteId(BanquetEventRouteBuilder.class));
    }

    @Test
    void convertsMultiWordClassNameToKebabCase() {
        assertEquals("royal-albert-hall-pipeline", RouteIdDeriver.deriveRouteId(RoyalAlbertHallEventRouteBuilder.class));
    }
}
