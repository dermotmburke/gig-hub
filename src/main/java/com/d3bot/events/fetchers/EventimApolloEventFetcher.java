package com.d3bot.events.fetchers;

import com.d3bot.events.utilities.UrlFetcher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty("fetchers.ticketmaster.api-key")
public class EventimApolloEventFetcher extends TicketmasterEventFetcher {

    public EventimApolloEventFetcher(
            UrlFetcher urlFetcher,
            @Value("${fetchers.ticketmaster.api-key}") String apiKey,
            @Value("${fetchers.ticketmaster.venues.royalfestivalhall.id:KovZpZAtadaA}") String venueId) {
        super(urlFetcher, venueId, apiKey);
    }
}
