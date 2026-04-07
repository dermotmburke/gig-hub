package com.d3bot.events.notifiers;

import com.d3bot.events.models.Event;

import java.util.List;

public interface EventNotifier {
    void notify(List<Event> events);
}
