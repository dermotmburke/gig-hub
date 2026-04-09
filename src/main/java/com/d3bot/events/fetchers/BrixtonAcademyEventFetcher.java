package com.d3bot.events.fetchers;

import com.d3bot.events.utilities.UrlFetcher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty("fetchers.ticketmaster.api-key")
public class BrixtonAcademyEventFetcher extends TicketmasterEventFetcher {

    public BrixtonAcademyEventFetcher(
            UrlFetcher urlFetcher,
            @Value("${fetchers.ticketmaster.api-key}") String apiKey,
            @Value("${fetchers.ticketmaster.venues.royalfestivalhall.id:KovZ91777af}") String venueId) {
        super(urlFetcher, venueId, apiKey);
    }
}
