package com.d3bot.events;

import java.util.List;

public interface EventNotifier {
    void notify(List<Event> events);
}
