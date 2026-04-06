package com.d3bot.events;

import java.net.URI;
import java.util.List;

public class Main {
    public static void main(String[] args) throws Exception {
        EventExtractor eventExtractor =  new EventExtractor();
        var url = new URI("https://www.banquetrecords.com/events?w=1000").toURL();
        String content;
        try (var is = url.openStream()) {
            content = new String(is.readAllBytes());
        }
        List<Event> events = eventExtractor.extract(content);
        System.out.println(events);
    }
}