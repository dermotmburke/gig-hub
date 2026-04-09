package com.d3bot.events.utilities;

public class RouteIdBuilder {

    private RouteIdBuilder() {}

    public static String build(Class<?> clazz) {
        String name = clazz.getSimpleName().replace("EventRouteBuilder", "");
        return name.replaceAll("([a-z])([A-Z])", "$1-$2").toLowerCase() + "-pipeline";
    }
}
