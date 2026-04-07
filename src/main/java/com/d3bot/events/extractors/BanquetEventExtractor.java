package com.d3bot.events.extractors;

import com.d3bot.events.models.Event;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

@Service
public class BanquetEventExtractor {

    private static final Pattern DATE_PATTERN = Pattern.compile("(\\d+)(?:st|nd|rd|th)\\s+(\\w+)");
    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d+:\\d+[ap]m)", Pattern.CASE_INSENSITIVE);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mma", Locale.ENGLISH);

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
        var date = getDate(titleParts[0]);
        if (date == null) {
            return null;
        }
        return new Event(getArtist(artistEl), getLocation(titleParts[1]), date, getStartTime(titleParts[1]), href.getValue());
    }

    private String getArtist(Element artistEl) {
        return artistEl.text();
    }

    private LocalDate getDate(String datePart) {
        var matcher = DATE_PATTERN.matcher(datePart);
        if (!matcher.find()) {
            return null;
        }
        int day = Integer.parseInt(matcher.group(1));
        Month month = Month.valueOf(matcher.group(2).toUpperCase());
        return LocalDate.of(Year.now().getValue(), month, day);
    }

    private LocalTime getStartTime(String locationPart) {
        var matcher = TIME_PATTERN.matcher(locationPart);
        if (!matcher.find()) {
            return null;
        }
        return LocalTime.parse(matcher.group(1).toUpperCase(), TIME_FORMATTER);
    }

    private String getLocation(String locationPart) {
        return locationPart.split(",")[0];
    }
}
