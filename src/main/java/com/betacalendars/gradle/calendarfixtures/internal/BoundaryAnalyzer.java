package com.betacalendars.gradle.calendarfixtures.internal;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/** Reports dates that commonly expose calendar boundary defects. */
public final class BoundaryAnalyzer {
    private static final WeekFields ISO = WeekFields.ISO;

    private BoundaryAnalyzer() { }

    public static String yearJson(int year) {
        List<LocalDate> dates = boundaryDates(year);
        StringBuilder json = new StringBuilder("{\n  \"schemaVersion\": 1,\n  \"year\": ").append(year)
                .append(",\n  \"leapYear\": ").append(Year.isLeap(year)).append(",\n  \"dates\": [\n");
        for (int i = 0; i < dates.size(); i++) {
            LocalDate date = dates.get(i);
            json.append("    {\"date\":\"").append(date).append("\",\"kind\":\"").append(kind(date, year)).append("\",\"dayOfWeek\":\"")
                    .append(date.getDayOfWeek()).append("\",\"isoWeek\":").append(date.get(ISO.weekOfWeekBasedYear()))
                    .append(",\"isoWeekYear\":").append(date.get(ISO.weekBasedYear())).append('}')
                    .append(i + 1 == dates.size() ? "\n" : ",\n");
        }
        return json.append("  ]\n}\n").toString();
    }

    public static String rangeJson(LocalDate from, LocalDate to) {
        TreeSet<LocalDate> dates = new TreeSet<>();
        dates.add(from);
        dates.add(to);
        addNearbyIsoTransitions(from, to, dates);
        StringBuilder json = new StringBuilder("{\n  \"schemaVersion\": 1,\n  \"from\":\"").append(from)
                .append("\",\n  \"to\":\"").append(to).append("\",\n  \"dates\": [\n");
        int index = 0;
        for (LocalDate date : dates) {
            json.append("    {\"date\":\"").append(date).append("\",\"dayOfWeek\":\"").append(date.getDayOfWeek())
                    .append("\",\"isoWeek\":").append(date.get(ISO.weekOfWeekBasedYear()))
                    .append(",\"isoWeekYear\":").append(date.get(ISO.weekBasedYear())).append('}')
                    .append(++index == dates.size() ? "\n" : ",\n");
        }
        return json.append("  ]\n}\n").toString();
    }

    public static String markdown(List<Integer> years, List<String> ranges) {
        TreeSet<Integer> orderedYears = new TreeSet<>(years);
        StringBuilder md = new StringBuilder("# Calendar boundary report\n\n");
        if (!orderedYears.isEmpty()) {
            md.append("## Year boundaries\n\n");
            for (int year : orderedYears) {
                md.append("### ").append(year).append("\n\n")
                        .append("- Gregorian leap year: ").append(Year.isLeap(year)).append('\n')
                        .append("- January 1: ").append(LocalDate.of(year, 1, 1)).append(" (").append(LocalDate.of(year, 1, 1).getDayOfWeek()).append(")\n")
                        .append("- December 31: ").append(LocalDate.of(year, 12, 31)).append(" (").append(LocalDate.of(year, 12, 31).getDayOfWeek()).append(")\n")
                        .append("- February 29: ").append(Year.isLeap(year) ? "present" : "not present").append('\n')
                        .append("- ISO week-year transitions: ").append(isoTransitions(year)).append("\n\n")
                        .append("| Month | First date | First weekday | Last date | Last weekday | Rows (Mon-first) |\n")
                        .append("| --- | --- | --- | --- | --- | ---: |\n");
                for (int month = 1; month <= 12; month++) {
                    YearMonth ym = YearMonth.of(year, month);
                    LocalDate first = ym.atDay(1);
                    LocalDate last = ym.atEndOfMonth();
                    int rows = (Math.floorMod(first.getDayOfWeek().getValue() - DayOfWeek.MONDAY.getValue(), 7) + ym.lengthOfMonth() + 6) / 7;
                    md.append("| ").append(ym.getMonth()).append(" | ").append(first).append(" | ").append(first.getDayOfWeek())
                            .append(" | ").append(last).append(" | ").append(last.getDayOfWeek()).append(" | ").append(rows).append(" |\n");
                }
                md.append('\n');
            }
        }
        if (!ranges.isEmpty()) {
            md.append("## Date ranges\n\n| Start | End | Inclusive days | ISO week-year transitions |\n| --- | --- | ---: | --- |\n");
            for (int i = 0; i + 1 < ranges.size(); i += 2) {
                LocalDate from = LocalDate.parse(ranges.get(i));
                LocalDate to = LocalDate.parse(ranges.get(i + 1));
                md.append("| ").append(from).append(" | ").append(to).append(" | ")
                        .append(java.time.temporal.ChronoUnit.DAYS.between(from, to) + 1).append(" | ")
                        .append(transitionsBetween(from, to)).append(" |\n");
            }
            md.append('\n');
        }
        return md.toString();
    }

    private static List<LocalDate> boundaryDates(int year) {
        TreeSet<LocalDate> dates = new TreeSet<>();
        dates.add(LocalDate.of(year, 1, 1));
        dates.add(LocalDate.of(year, 12, 31));
        if (Year.isLeap(year)) dates.add(LocalDate.of(year, 2, 29));
        for (int month = 1; month <= 12; month++) {
            dates.add(YearMonth.of(year, month).atDay(1));
            dates.add(YearMonth.of(year, month).atEndOfMonth());
        }
        if (year > 1) {
            LocalDate date = LocalDate.of(year, 1, 1);
            LocalDate end = LocalDate.of(year, 12, 31);
            while (!date.isAfter(end)) {
                if (!date.isEqual(LocalDate.of(year, 1, 1)) && date.get(ISO.weekBasedYear()) != date.minusDays(1).get(ISO.weekBasedYear())) dates.add(date);
                date = date.plusDays(1);
            }
        }
        return List.copyOf(dates);
    }

    private static void addNearbyIsoTransitions(LocalDate from, LocalDate to, TreeSet<LocalDate> dates) {
        LocalDate date = from.plusDays(1);
        while (!date.isAfter(to)) {
            if (date.get(ISO.weekBasedYear()) != date.minusDays(1).get(ISO.weekBasedYear())) dates.add(date);
            date = date.plusDays(1);
        }
    }

    private static String isoTransitions(int year) {
        List<String> found = new ArrayList<>();
        LocalDate date = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);
        while (!date.isAfter(end)) {
            if (year > 1 && !date.equals(LocalDate.of(year, 1, 1)) && date.get(ISO.weekBasedYear()) != date.minusDays(1).get(ISO.weekBasedYear())) {
                found.add(date + " → " + date.get(ISO.weekBasedYear()) + "-W" + date.get(ISO.weekOfWeekBasedYear()));
            }
            date = date.plusDays(1);
        }
        return found.isEmpty() ? "none" : String.join(", ", found);
    }

    private static String transitionsBetween(LocalDate from, LocalDate to) {
        List<String> found = new ArrayList<>();
        LocalDate date = from.plusDays(1);
        while (!date.isAfter(to)) {
            if (date.get(ISO.weekBasedYear()) != date.minusDays(1).get(ISO.weekBasedYear())) found.add(date.toString());
            date = date.plusDays(1);
        }
        return found.isEmpty() ? "none" : String.join(", ", found);
    }

    private static String kind(LocalDate date, int year) {
        if (date.equals(LocalDate.of(year, 1, 1))) return "year-start";
        if (date.equals(LocalDate.of(year, 12, 31))) return "year-end";
        if (Year.isLeap(year) && date.equals(LocalDate.of(year, 2, 29))) return "leap-day";
        if (date.getDayOfMonth() == 1) return "month-start";
        if (date.equals(date.withDayOfMonth(date.lengthOfMonth()))) return "month-end";
        return "iso-week-year-transition";
    }
}
