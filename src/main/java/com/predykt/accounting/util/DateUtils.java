// ============================================
// DateUtils.java
// ============================================
package com.predykt.accounting.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

public class DateUtils {
    
    private DateUtils() {}
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    
    public static String formatDate(LocalDate date) {
        return date != null ? date.format(DATE_FORMATTER) : "";
    }
    
    public static String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATETIME_FORMATTER) : "";
    }
    
    public static LocalDate parseDate(String dateStr) {
        return LocalDate.parse(dateStr, DATE_FORMATTER);
    }
    
    public static boolean isWeekend(LocalDate date) {
        return date.getDayOfWeek().getValue() >= 6;
    }
    
    public static boolean isSameMonth(LocalDate date1, LocalDate date2) {
        return date1.getYear() == date2.getYear() && date1.getMonth() == date2.getMonth();
    }
    
    public static List<LocalDate> getMonthDates(LocalDate date) {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate start = date.with(TemporalAdjusters.firstDayOfMonth());
        LocalDate end = date.with(TemporalAdjusters.lastDayOfMonth());
        
        while (!start.isAfter(end)) {
            dates.add(start);
            start = start.plusDays(1);
        }
        
        return dates;
    }
    
    public static long getBusinessDaysBetween(LocalDate startDate, LocalDate endDate) {
        long days = 0;
        LocalDate current = startDate;
        
        while (!current.isAfter(endDate)) {
            if (!isWeekend(current)) {
                days++;
            }
            current = current.plusDays(1);
        }
        
        return days;
    }
}