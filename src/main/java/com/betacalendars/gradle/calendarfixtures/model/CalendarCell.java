package com.betacalendars.gradle.calendarfixtures.model;

import java.time.LocalDate;

public record CalendarCell(
        LocalDate date,
        int row,
        int column,
        boolean inRequestedMonth,
        Integer weekOfWeekBasedYear,
        Integer weekBasedYear) {
}
