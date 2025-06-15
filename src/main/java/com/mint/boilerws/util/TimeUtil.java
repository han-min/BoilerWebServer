package com.mint.boilerws.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class TimeUtil {

    private static final ZoneId ZONE_ID = ZoneId.systemDefault();

    public static final long ONE_DAY_MS = 24 * 60 * 60 * 1000;
    
    public static DateTimeFormatter getDateTimeFormatter() {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of("Europe/London"));
    }
    
    public static LocalTime getLocalTime(final long time) {
        final LocalDateTime localDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(time), ZONE_ID);
        return localDateTime.toLocalTime();
    }

    public static long getTomorrowStartOfDay(final long epochMs) {
        final LocalDate date = Instant.ofEpochMilli(epochMs).atZone(ZONE_ID).toLocalDate();
        final LocalDate tomorrow = date.plusDays(1);
        return tomorrow.atStartOfDay(ZONE_ID).toInstant().toEpochMilli();
    }
    
    public static boolean isWeekend(final long time) {
        final LocalDate date = Instant.ofEpochMilli(time).atZone(ZONE_ID).toLocalDate();
        switch (date.getDayOfWeek()) {
        case SATURDAY:
        case SUNDAY:
            return true;
        default:
            return false;
        }
    }

    public static boolean isWeekday(final long time) {
        return !isWeekend(time);
    }

}
