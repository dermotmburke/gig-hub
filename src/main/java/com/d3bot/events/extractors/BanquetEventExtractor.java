package com.d3bot.events.extractors;

import com.d3bot.events.models.Event;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
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

    private final Clock clock;

    public BanquetEventExtractor() {
        this(Clock.systemDefaultZone());
    }

    public BanquetEventExtractor(Clock clock) {
        this.clock = clock;
    }

    public List<Event> extract(String page) {
        return Jsoup.parse(page, BASE_URL).select("a.card").stream()
                .map(this::parseCard)
                .filter(Objects::nonNull)
                .toList();
    }

    private Event parseCard(Element card) {
        var artistElement = card.selectFirst("span.artist");
        var titleElement = card.selectFirst("span.title");
        var url = card.absUrl("href");
        if (artistElement == null || titleElement == null || url.isEmpty()) {
            return null;
        }
        var titleParts = titleElement.text().split(" at ", 2);
        if (titleParts.length < 2) {
            return null;
        }
        var dateTime = getDateTime(titleParts[0], titleParts[1]);
        if (dateTime == null) {
            return null;
        }
        return new Event(getArtist(artistElement), getLocation(titleParts[1]), dateTime, url);
    }

    private String getArtist(Element artistEl) {
        return artistEl.text();
    }

    private LocalDateTime getDateTime(String datePart, String locationPart) {
        var dateMatcher = DATE_PATTERN.matcher(datePart);
        if (!dateMatcher.find()) {
            return null;
        }
        try {
            int day = Integer.parseInt(dateMatcher.group(1));
            Month month = Month.valueOf(dateMatcher.group(2).toUpperCase());
            // Banquet omits the year and only lists upcoming gigs, so a date already past is next year's
            LocalDate today = LocalDate.now(clock);
            LocalDate date = LocalDate.of(today.getYear(), month, day);
            if (date.isBefore(today)) {
                date = date.plusYears(1);
            }

            var timeMatcher = TIME_PATTERN.matcher(locationPart);
            LocalTime time = timeMatcher.find()
                    ? LocalTime.parse(timeMatcher.group(1).toUpperCase(), TIME_FORMATTER)
                    : LocalTime.MIDNIGHT;

            return LocalDateTime.of(date, time);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private String getLocation(String locationPart) {
        return locationPart.split(",")[0];
    }
}
