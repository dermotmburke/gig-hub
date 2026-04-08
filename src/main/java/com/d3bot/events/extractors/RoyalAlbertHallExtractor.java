package com.d3bot.events.extractors;

import org.springframework.stereotype.Service;

/**
 * Ticketmaster extractor for Royal Albert Hall events.
 * Override any hook methods from {@link TicketmasterEventExtractor} here to
 * apply Royal Albert Hall-specific parsing (e.g. canonical venue name,
 * custom artist name formatting).
 */
@Service
public class RoyalAlbertHallExtractor extends TicketmasterEventExtractor {
}
