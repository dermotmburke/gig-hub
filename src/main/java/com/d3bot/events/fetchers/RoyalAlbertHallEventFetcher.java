package com.d3bot.events.fetchers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty("fetchers.ticketmaster.api-key")
public class RoyalAlbertHallEventFetcher extends TicketmasterEventFetcher {

    public RoyalAlbertHallEventFetcher(
            UrlFetcher urlFetcher,
            @Value("${fetchers.ticketmaster.api-key}") String apiKey,
            @Value("${fetchers.ticketmaster.venues.royalalberthall.id:KovZ9177Arf}") String venueId) {
        super(urlFetcher, venueId, apiKey);
    }
}
