package com.d3bot.events.utilities;

import com.d3bot.events.models.Event;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;

@Component
@ConditionalOnProperty("gig-hub-calendar.base-url")
public class GigHubCalendarUrlBuilder {

    private final String baseUrl;

    public GigHubCalendarUrlBuilder(@Value("${gig-hub-calendar.base-url}") String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String build(Event event) {
        return baseUrl + "/save"
                + "?artist=" + encode(event.artist())
                + "&location=" + encode(event.location())
                + "&date=" + event.dateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                + "&url=" + encode(event.url());
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
