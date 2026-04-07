package com.d3bot.events.models;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;

public record Event(String artist, String location, LocalDateTime dateTime, String url) {

    private static final DateTimeFormatter FORMATTER = ISO_LOCAL_DATE_TIME;

    public String checksum() {
        String input = String.join("|",
                normalise(location),
                normalise(FORMATTER.format(dateTime))
        );
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate checksum", e);
        }
    }

    public String key() {
        return "banquet:event:" + checksum();
    }

    private String normalise(String value) {
        return value.trim().toLowerCase();
    }
}
