package com.d3bot.events.utilities;

import com.d3bot.events.routes.BanquetEventRouteBuilder;
import com.d3bot.events.routes.RoyalAlbertHallEventRouteBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RouteIdBuilderTest {

    @Test
    void stripsEventRouteBuilderSuffix() {
        assertEquals("banquet-pipeline", RouteIdBuilder.build(BanquetEventRouteBuilder.class));
    }

    @Test
    void convertsMultiWordClassNameToKebabCase() {
        assertEquals("royal-albert-hall-pipeline", RouteIdBuilder.build(RoyalAlbertHallEventRouteBuilder.class));
    }
}
