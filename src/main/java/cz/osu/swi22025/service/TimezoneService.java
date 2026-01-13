package cz.osu.swi22025.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class TimezoneService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    public String convertToUserTimezone(Instant instant, String timezone) {
        if (timezone == null || timezone.isEmpty()) {
            timezone = "UTC";
        }
        
        try {
            ZoneId zoneId = ZoneId.of(timezone);
            ZonedDateTime userTime = instant.atZone(zoneId);
            return userTime.format(FORMATTER);
        } catch (Exception e) {
            // Fallback na UTC, pokud je zóna neplatná
            return instant.atZone(ZoneId.of("UTC")).format(FORMATTER);
        }
    }

    public String getCurrentTimeInTimezone(String timezone) {
        if (timezone == null || timezone.isEmpty()) {
            timezone = "UTC";
        }
        
        try {
            ZoneId zoneId = ZoneId.of(timezone);
            ZonedDateTime now = ZonedDateTime.now(zoneId);
            return now.format(FORMATTER);
        } catch (Exception e) {
            return ZonedDateTime.now(ZoneId.of("UTC")).format(FORMATTER);
        }
    }

    public Instant parseFromUserTimezone(String dateTimeString, String timezone) {
        if (timezone == null || timezone.isEmpty()) {
            timezone = "UTC";
        }
        
        try {
            ZoneId zoneId = ZoneId.of(timezone);
            ZonedDateTime userTime = ZonedDateTime.parse(dateTimeString, FORMATTER.withZone(zoneId));
            return userTime.toInstant();
        } catch (Exception e) {
            // Fallback parsing (kdyby přišel jiný formát)
            return Instant.now();
        }
    }
}