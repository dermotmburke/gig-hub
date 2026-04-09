package com.d3bot.events.fetchers;

import com.d3bot.events.utilities.UrlFetcher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty("fetchers.ticketmaster.api-key")
public class RoyalFestivalHallEventFetcher extends TicketmasterEventFetcher {

    public RoyalFestivalHallEventFetcher(
            UrlFetcher urlFetcher,
            @Value("${fetchers.ticketmaster.api-key}") String apiKey,
            @Value("${fetchers.ticketmaster.venues.royalfestivalhall.id:KovZpZAnFvlA}") String venueId) {
        super(urlFetcher, venueId, apiKey);
    }
}
