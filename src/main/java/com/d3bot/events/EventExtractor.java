package com.d3bot.events;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

import java.util.List;
import java.util.Objects;

public class EventExtractor {
    public List<Event> extract(String page) {
        return Jsoup.parse(page).select("a.card").stream()
                .map(this::parseCard)
                .filter(Objects::nonNull)
                .toList();
    }

    private Event parseCard(Element card) {
        var artistEl = card.selectFirst("span.artist");
        var titleEl = card.selectFirst("span.title");
        var href = card.attribute("href");
        if (artistEl == null || titleEl == null || href == null) {
            return null;
        }
        var titleParts = titleEl.text().split(" at ", 2);
        if (titleParts.length < 2) {
            return null;
        }
        return new Event(getArtist(artistEl), getLocation(titleParts[1]), getDate(titleParts[0]), href.getValue());
    }

    private String getArtist(Element artistEl) {
        return artistEl.text();
    }

    private String getDate(String datePart) {
        return datePart;
    }

    private String getLocation(String locationPart) {
        return locationPart.split(",")[0];
    }
}
