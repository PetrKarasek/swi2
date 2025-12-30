package cz.osu.swi22025.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class TimezoneService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public String convertToUserTimezone(Instant instant, String timezone) {
        if (timezone == null || timezone.isEmpty()) {
            timezone = "UTC";
        }
        
        ZoneId zoneId = ZoneId.of(timezone);
        ZonedDateTime userTime = instant.atZone(zoneId);
        return userTime.format(FORMATTER);
    }

    public String getCurrentTimeInTimezone(String timezone) {
        if (timezone == null || timezone.isEmpty()) {
            timezone = "UTC";
        }
        
        ZoneId zoneId = ZoneId.of(timezone);
        ZonedDateTime now = ZonedDateTime.now(zoneId);
        return now.format(FORMATTER);
    }

    public Instant parseFromUserTimezone(String dateTimeString, String timezone) {
        if (timezone == null || timezone.isEmpty()) {
            timezone = "UTC";
        }
        
        ZoneId zoneId = ZoneId.of(timezone);
        ZonedDateTime userTime = ZonedDateTime.parse(dateTimeString, FORMATTER.withZone(zoneId));
        return userTime.toInstant();
    }
}
