package com.d3bot.events.extractors;

import com.d3bot.events.models.Event;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

@Service
public class BanquetEventExtractor implements EventExtractor {

    private static final Pattern DATE_PATTERN = Pattern.compile("(\\d+)(?:st|nd|rd|th)\\s+(\\w+)");
    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d+:\\d+[ap]m)", Pattern.CASE_INSENSITIVE);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mma", Locale.ENGLISH);

    private static final String BASE_URL = "https://www.banquetrecords.com";

    public List<Event> extract(String page) {
        return Jsoup.parse(page, BASE_URL).select("a.card").stream()
                .map(this::parseCard)
                .filter(Objects::nonNull)
                .toList();
    }

    private Event parseCard(Element card) {
        var artistEl = card.selectFirst("span.artist");
        var titleEl = card.selectFirst("span.title");
        var url = card.absUrl("href");
        if (artistEl == null || titleEl == null || url.isEmpty()) {
            return null;
        }
        var titleParts = titleEl.text().split(" at ", 2);
        if (titleParts.length < 2) {
            return null;
        }
        var dateTime = getDateTime(titleParts[0], titleParts[1]);
        if (dateTime == null) {
            return null;
        }
        return new Event(getArtist(artistEl), getLocation(titleParts[1]), dateTime, url);
    }

    private String getArtist(Element artistEl) {
        return artistEl.text();
    }

    private LocalDateTime getDateTime(String datePart, String locationPart) {
        var dateMatcher = DATE_PATTERN.matcher(datePart);
        if (!dateMatcher.find()) {
            return null;
        }
        int day = Integer.parseInt(dateMatcher.group(1));
        Month month = Month.valueOf(dateMatcher.group(2).toUpperCase());
        LocalDate date = LocalDate.of(Year.now().getValue(), month, day);

        var timeMatcher = TIME_PATTERN.matcher(locationPart);
        LocalTime time = timeMatcher.find()
                ? LocalTime.parse(timeMatcher.group(1).toUpperCase(), TIME_FORMATTER)
                : LocalTime.MIDNIGHT;

        return LocalDateTime.of(date, time);
    }

    private String getLocation(String locationPart) {
        return locationPart.split(",")[0];
    }
}
