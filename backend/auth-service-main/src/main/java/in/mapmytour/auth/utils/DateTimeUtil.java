package in.mapmytour.auth.utils;

import lombok.extern.slf4j.Slf4j;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

@Slf4j
public class DateTimeUtil {

    public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";
    public static final String DEFAULT_DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final String ISO_DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    public static final String DISPLAY_DATE_FORMAT = "dd MMM yyyy";
    public static final String DISPLAY_DATETIME_FORMAT = "dd MMM yyyy, HH:mm";

    public static final ZoneId DEFAULT_ZONE_ID = ZoneId.of("Asia/Kolkata");

    // Get current date and time
    public static LocalDateTime getCurrentDateTime() {
        return LocalDateTime.now(DEFAULT_ZONE_ID);
    }

    public static LocalDate getCurrentDate() {
        return LocalDate.now(DEFAULT_ZONE_ID);
    }

    public static LocalTime getCurrentTime() {
        return LocalTime.now(DEFAULT_ZONE_ID);
    }

    public static ZonedDateTime getCurrentZonedDateTime() {
        return ZonedDateTime.now(DEFAULT_ZONE_ID);
    }

    public static long getCurrentTimestamp() {
        return System.currentTimeMillis();
    }

    // Formatting methods
    public static String formatDate(LocalDate date) {
        return formatDate(date, DEFAULT_DATE_FORMAT);
    }

    public static String formatDate(LocalDate date, String pattern) {
        if (date == null) return null;
        try {
            return date.format(DateTimeFormatter.ofPattern(pattern));
        } catch (Exception e) {
            log.error("Error formatting date: {}", e.getMessage());
            return null;
        }
    }

    public static String formatDateTime(LocalDateTime dateTime) {
        return formatDateTime(dateTime, DEFAULT_DATETIME_FORMAT);
    }

    public static String formatDateTime(LocalDateTime dateTime, String pattern) {
        if (dateTime == null) return null;
        try {
            return dateTime.format(DateTimeFormatter.ofPattern(pattern));
        } catch (Exception e) {
            log.error("Error formatting datetime: {}", e.getMessage());
            return null;
        }
    }

    public static String formatForDisplay(LocalDate date) {
        return formatDate(date, DISPLAY_DATE_FORMAT);
    }

    public static String formatForDisplay(LocalDateTime dateTime) {
        return formatDateTime(dateTime, DISPLAY_DATETIME_FORMAT);
    }

    // Parsing methods
    public static LocalDate parseDate(String dateString) {
        return parseDate(dateString, DEFAULT_DATE_FORMAT);
    }

    public static LocalDate parseDate(String dateString, String pattern) {
        if (dateString == null || dateString.trim().isEmpty()) return null;
        try {
            return LocalDate.parse(dateString, DateTimeFormatter.ofPattern(pattern));
        } catch (DateTimeParseException e) {
            log.error("Error parsing date '{}' with pattern '{}': {}", dateString, pattern, e.getMessage());
            return null;
        }
    }

    public static LocalDateTime parseDateTime(String dateTimeString) {
        return parseDateTime(dateTimeString, DEFAULT_DATETIME_FORMAT);
    }

    public static LocalDateTime parseDateTime(String dateTimeString, String pattern) {
        if (dateTimeString == null || dateTimeString.trim().isEmpty()) return null;
        try {
            return LocalDateTime.parse(dateTimeString, DateTimeFormatter.ofPattern(pattern));
        } catch (DateTimeParseException e) {
            log.error("Error parsing datetime '{}' with pattern '{}': {}", dateTimeString, pattern, e.getMessage());
            return null;
        }
    }

    // Conversion methods
    public static LocalDateTime timestampToDateTime(long timestamp) {
        try {
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), DEFAULT_ZONE_ID);
        } catch (Exception e) {
            log.error("Error converting timestamp to datetime: {}", e.getMessage());
            return null;
        }
    }

    public static long dateTimeToTimestamp(LocalDateTime dateTime) {
        if (dateTime == null) return 0;
        try {
            return dateTime.atZone(DEFAULT_ZONE_ID).toInstant().toEpochMilli();
        } catch (Exception e) {
            log.error("Error converting datetime to timestamp: {}", e.getMessage());
            return 0;
        }
    }

    // Age calculation
    public static int calculateAge(LocalDate birthDate) {
        if (birthDate == null) return 0;
        return Period.between(birthDate, getCurrentDate()).getYears();
    }

    // Date comparison and validation
    public static boolean isDateInPast(LocalDate date) {
        return date != null && date.isBefore(getCurrentDate());
    }

    public static boolean isDateInFuture(LocalDate date) {
        return date != null && date.isAfter(getCurrentDate());
    }

    public static boolean isDateTimeInPast(LocalDateTime dateTime) {
        return dateTime != null && dateTime.isBefore(getCurrentDateTime());
    }

    public static boolean isDateTimeInFuture(LocalDateTime dateTime) {
        return dateTime != null && dateTime.isAfter(getCurrentDateTime());
    }

    public static boolean isToday(LocalDate date) {
        return date != null && date.equals(getCurrentDate());
    }

    public static boolean isTomorrow(LocalDate date) {
        return date != null && date.equals(getCurrentDate().plusDays(1));
    }

    public static boolean isYesterday(LocalDate date) {
        return date != null && date.equals(getCurrentDate().minusDays(1));
    }

    // Duration and period calculations
    public static long daysBetween(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) return 0;
        return ChronoUnit.DAYS.between(startDate, endDate);
    }

    public static long hoursBetween(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        if (startDateTime == null || endDateTime == null) return 0;
        return ChronoUnit.HOURS.between(startDateTime, endDateTime);
    }

    public static long minutesBetween(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        if (startDateTime == null || endDateTime == null) return 0;
        return ChronoUnit.MINUTES.between(startDateTime, endDateTime);
    }

    public static long secondsBetween(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        if (startDateTime == null || endDateTime == null) return 0;
        return ChronoUnit.SECONDS.between(startDateTime, endDateTime);
    }

    // Date range methods
    public static boolean isDateInRange(LocalDate date, LocalDate startDate, LocalDate endDate) {
        if (date == null || startDate == null || endDate == null) return false;
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    public static boolean isDateTimeInRange(LocalDateTime dateTime, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        if (dateTime == null || startDateTime == null || endDateTime == null) return false;
        return !dateTime.isBefore(startDateTime) && !dateTime.isAfter(endDateTime);
    }

    // Start and end of day/month/year
    public static LocalDateTime startOfDay(LocalDate date) {
        return date != null ? date.atStartOfDay() : null;
    }

    public static LocalDateTime endOfDay(LocalDate date) {
        return date != null ? date.atTime(23, 59, 59, 999999999) : null;
    }

    public static LocalDate startOfMonth(LocalDate date) {
        return date != null ? date.withDayOfMonth(1) : null;
    }

    public static LocalDate endOfMonth(LocalDate date) {
        return date != null ? date.withDayOfMonth(date.lengthOfMonth()) : null;
    }

    public static LocalDate startOfYear(LocalDate date) {
        return date != null ? date.withDayOfYear(1) : null;
    }

    public static LocalDate endOfYear(LocalDate date) {
        return date != null ? date.withDayOfYear(date.lengthOfYear()) : null;
    }

    // Timezone conversion
    public static ZonedDateTime convertToTimeZone(LocalDateTime dateTime, String timeZoneId) {
        if (dateTime == null || timeZoneId == null) return null;
        try {
            ZoneId zoneId = ZoneId.of(timeZoneId);
            return dateTime.atZone(DEFAULT_ZONE_ID).withZoneSameInstant(zoneId);
        } catch (Exception e) {
            log.error("Error converting to timezone '{}': {}", timeZoneId, e.getMessage());
            return null;
        }
    }

    // Business day calculations
    public static boolean isWeekend(LocalDate date) {
        if (date == null) return false;
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    }

    public static boolean isWeekday(LocalDate date) {
        return !isWeekend(date);
    }

    public static LocalDate getNextBusinessDay(LocalDate date) {
        if (date == null) return null;
        LocalDate nextDay = date.plusDays(1);
        while (isWeekend(nextDay)) {
            nextDay = nextDay.plusDays(1);
        }
        return nextDay;
    }

    public static LocalDate getPreviousBusinessDay(LocalDate date) {
        if (date == null) return null;
        LocalDate previousDay = date.minusDays(1);
        while (isWeekend(previousDay)) {
            previousDay = previousDay.minusDays(1);
        }
        return previousDay;
    }

    // Utility methods for common date operations
    public static String getTimeAgo(LocalDateTime dateTime) {
        if (dateTime == null) return "Unknown";

        LocalDateTime now = getCurrentDateTime();
        long minutes = minutesBetween(dateTime, now);

        if (minutes < 1) return "Just now";
        if (minutes < 60) return minutes + " minutes ago";

        long hours = hoursBetween(dateTime, now);
        if (hours < 24) return hours + " hours ago";

        long days = daysBetween(dateTime.toLocalDate(), now.toLocalDate());
        if (days == 1) return "Yesterday";
        if (days < 7) return days + " days ago";
        if (days < 30) return (days / 7) + " weeks ago";
        if (days < 365) return (days / 30) + " months ago";

        return (days / 365) + " years ago";
    }

    public static boolean isValidDateRange(LocalDate startDate, LocalDate endDate) {
        return startDate != null && endDate != null && !startDate.isAfter(endDate);
    }

    public static boolean isValidDateTimeRange(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        return startDateTime != null && endDateTime != null && !startDateTime.isAfter(endDateTime);
    }
}