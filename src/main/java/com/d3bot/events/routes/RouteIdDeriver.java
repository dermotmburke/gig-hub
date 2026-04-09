package com.d3bot.events.routes;

public class RouteIdDeriver {

    private RouteIdDeriver() {}

    public static String deriveRouteId(Class<?> clazz) {
        String name = clazz.getSimpleName().replace("EventRouteBuilder", "");
        return name.replaceAll("([a-z])([A-Z])", "$1-$2").toLowerCase() + "-pipeline";
    }
}
