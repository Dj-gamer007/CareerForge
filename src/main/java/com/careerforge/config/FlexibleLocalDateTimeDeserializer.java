package com.careerforge.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Robust Jackson deserializer for LocalDateTime that parses:
 * - ISO-8601 UTC instants with 'Z' (e.g. "2026-08-27T04:58:00.000Z")
 * - ISO-8601 offset strings (e.g. "2026-08-27T10:28:00+05:30")
 * - ISO-8601 local timestamps (e.g. "2026-08-27T04:58:00" or "2026-08-27T10:28")
 * Converts any zoned/offset timestamp to an absolute UTC LocalDateTime.
 */
public class FlexibleLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {

    @Override
    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getText();
        if (!StringUtils.hasText(value)) {
            return null;
        }
        value = value.trim();

        // 1. Check for 'Z' or timezone offset (e.g. +05:30 or -04:00)
        if (value.endsWith("Z") || value.endsWith("z")) {
            try {
                Instant instant = Instant.parse(value);
                return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
            } catch (DateTimeParseException ignored) {
            }
        }
        if (value.matches(".*[+-]\\d{2}:?\\d{2}$")) {
            try {
                Instant instant = DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(value, Instant::from);
                return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
            } catch (DateTimeParseException ignored) {
            }
        }

        // 2. Try standard ISO_LOCAL_DATE_TIME (e.g. 2026-08-27T04:58:00)
        try {
            return LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException e) {
            try {
                // Try format without seconds: 2026-08-27T10:28
                return LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
            } catch (DateTimeParseException ex) {
                // Fallback to Instant parsing
                return LocalDateTime.ofInstant(Instant.parse(value), ZoneOffset.UTC);
            }
        }
    }
}
