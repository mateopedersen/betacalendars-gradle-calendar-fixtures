package com.betacalendars.gradle.calendarfixtures.internal;

import com.betacalendars.gradle.calendarfixtures.model.CalendarCell;
import com.betacalendars.gradle.calendarfixtures.model.CalendarOutputFormat;
import com.betacalendars.gradle.calendarfixtures.model.GridMode;

import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** Deterministic, dependency-free renderers for calendar fixtures. */
public final class CalendarGenerator {
    public static final int SCHEMA_VERSION = 1;
    private static final WeekFields ISO = WeekFields.ISO;
    private static final List<String> CSV_HEADER = List.of(
            "date", "year", "month", "day", "dayOfWeek", "dayOfYear",
            "weekOfWeekBasedYear", "weekBasedYear", "row", "column", "inRequestedMonth");

    private CalendarGenerator() { }

    public static Map<String, byte[]> generateYear(
            int year,
            DayOfWeek firstDayOfWeek,
            GridMode gridMode,
            boolean includeOverflowDays,
            boolean includeIsoWeekNumbers,
            Set<CalendarOutputFormat> formats,
            Map<String, String> metadata) {
        validateYear(year, "calendar fixture");
        requireFormats(formats, "calendar fixture");
        TreeMap<String, byte[]> output = new TreeMap<>();
        List<List<CalendarCell>> months = new ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            List<CalendarCell> cells = monthCells(year, month, firstDayOfWeek, gridMode, includeOverflowDays, includeIsoWeekNumbers);
            months.add(cells);
            String base = String.format(Locale.ROOT, "months/%02d", month);
            for (CalendarOutputFormat format : sortedFormats(formats)) {
                output.put(base + extension(format), renderMonth(year, month, firstDayOfWeek, gridMode, cells, format).getBytes(StandardCharsets.UTF_8));
            }
        }
        for (CalendarOutputFormat format : sortedFormats(formats)) {
            output.put("year" + extension(format), renderYear(year, firstDayOfWeek, gridMode, includeOverflowDays, includeIsoWeekNumbers, months, metadata, format).getBytes(StandardCharsets.UTF_8));
        }
        output.put("boundaries.json", BoundaryAnalyzer.yearJson(year).getBytes(StandardCharsets.UTF_8));
        output.put("manifest.json", manifest(year, "calendar-year", formats, metadata, output.keySet()).getBytes(StandardCharsets.UTF_8));
        return Collections.unmodifiableMap(output);
    }

    public static Map<String, byte[]> generateRange(
            LocalDate from,
            LocalDate to,
            DayOfWeek firstDayOfWeek,
            boolean includeIsoWeekNumbers,
            Set<CalendarOutputFormat> formats,
            Map<String, String> metadata) {
        if (from == null || to == null || to.isBefore(from)) {
            throw new IllegalArgumentException("Calendar range '" + from + "' to '" + to + "' is invalid. The start date must be on or before the end date.");
        }
        requireFormats(formats, "calendar range");
        long days = java.time.temporal.ChronoUnit.DAYS.between(from, to) + 1;
        if (days > 366_000) {
            throw new IllegalArgumentException("Calendar range contains " + days + " days. Ranges may not exceed 366000 days.");
        }
        List<CalendarCell> cells = new ArrayList<>();
        int offset = dayOffset(from.getDayOfWeek(), firstDayOfWeek);
        LocalDate date = from;
        for (int index = 0; index < days; index++, date = date.plusDays(1)) {
            int position = offset + index;
            cells.add(cell(date, position / 7, position % 7, true, includeIsoWeekNumbers));
        }
        TreeMap<String, byte[]> output = new TreeMap<>();
        for (CalendarOutputFormat format : sortedFormats(formats)) {
            output.put("range" + extension(format), renderRange(from, to, firstDayOfWeek, cells, metadata, format).getBytes(StandardCharsets.UTF_8));
        }
        output.put("boundaries.json", BoundaryAnalyzer.rangeJson(from, to).getBytes(StandardCharsets.UTF_8));
        output.put("manifest.json", manifest(from.getYear(), "date-range", formats, metadata, output.keySet()).getBytes(StandardCharsets.UTF_8));
        return Collections.unmodifiableMap(output);
    }

    public static List<CalendarCell> monthCells(int year, int month, DayOfWeek firstDayOfWeek, GridMode gridMode,
                                                 boolean includeOverflowDays, boolean includeIsoWeekNumbers) {
        validateYear(year, "calendar fixture");
        YearMonth yearMonth;
        try {
            yearMonth = YearMonth.of(year, month);
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Calendar fixture month " + month + " is invalid. Months must be between 1 and 12.", ex);
        }
        LocalDate first = yearMonth.atDay(1);
        int offset = dayOffset(first.getDayOfWeek(), firstDayOfWeek);
        int rows = gridMode == GridMode.FIXED_SIX_ROWS ? 6 : (offset + yearMonth.lengthOfMonth() + 6) / 7;
        List<CalendarCell> result = new ArrayList<>(rows * 7);
        LocalDate gridStart = first.minusDays(offset);
        for (int index = 0; index < rows * 7; index++) {
            int row = index / 7;
            int column = index % 7;
            LocalDate date = gridStart.plusDays(index);
            boolean inMonth = YearMonth.from(date).equals(yearMonth);
            LocalDate representedDate = inMonth || includeOverflowDays ? date : null;
            result.add(cell(representedDate, row, column, inMonth, includeIsoWeekNumbers));
        }
        return List.copyOf(result);
    }

    public static void validateYear(int year, String name) {
        if (year < 1 || year > 9999) {
            throw new IllegalArgumentException("Calendar fixture '" + name + "' has year " + year + ". Calendar fixture years must be between 1 and 9999.");
        }
    }

    private static CalendarCell cell(LocalDate date, int row, int column, boolean inMonth, boolean includeWeeks) {
        Integer week = date == null || !includeWeeks ? null : date.get(ISO.weekOfWeekBasedYear());
        Integer weekYear = date == null || !includeWeeks ? null : date.get(ISO.weekBasedYear());
        return new CalendarCell(date, row, column, inMonth, week, weekYear);
    }

    private static int dayOffset(DayOfWeek day, DayOfWeek first) {
        return Math.floorMod(day.getValue() - first.getValue(), 7);
    }

    private static String renderMonth(int year, int month, DayOfWeek first, GridMode mode, List<CalendarCell> cells, CalendarOutputFormat format) {
        return switch (format) {
            case JSON -> monthJson(year, month, first, mode, cells);
            case CSV -> csv(cells);
            case MARKDOWN -> markdown(YearMonth.of(year, month).getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH), first, cells);
        };
    }

    private static String renderYear(int year, DayOfWeek first, GridMode mode, boolean overflow, boolean iso,
                                     List<List<CalendarCell>> months, Map<String, String> metadata, CalendarOutputFormat format) {
        if (format == CalendarOutputFormat.CSV) {
            StringBuilder csv = new StringBuilder(String.join(",", CSV_HEADER)).append(",month\n");
            for (int month = 1; month <= 12; month++) {
                for (CalendarCell cell : months.get(month - 1)) {
                    csv.append(csvRow(cell)).append(',').append(month).append('\n');
                }
            }
            return csv.toString();
        }
        if (format == CalendarOutputFormat.MARKDOWN) {
            StringBuilder md = new StringBuilder("# Calendar ").append(year).append("\n\n");
            for (int month = 1; month <= 12; month++) {
                md.append("## ").append(YearMonth.of(year, month).getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH)).append("\n\n")
                        .append(markdownTable(first, months.get(month - 1))).append('\n');
            }
            return md.toString();
        }
        StringBuilder json = new StringBuilder("{\n  \"schemaVersion\": ").append(SCHEMA_VERSION)
                .append(",\n  \"year\": ").append(year)
                .append(",\n  \"leapYear\": ").append(Year.isLeap(year))
                .append(",\n  \"firstDayOfWeek\": ").append(quote(first.name()))
                .append(",\n  \"gridMode\": ").append(quote(mode.name()))
                .append(",\n  \"includeOverflowDays\": ").append(overflow)
                .append(",\n  \"includeIsoWeekNumbers\": ").append(iso)
                .append(",\n  \"metadata\": ").append(metadataJson(metadata))
                .append(",\n  \"months\": [\n");
        for (int month = 1; month <= 12; month++) {
            json.append("    ").append(monthObject(year, month, first, mode, months.get(month - 1)));
            json.append(month == 12 ? "\n" : ",\n");
        }
        return json.append("  ]\n}\n").toString();
    }

    private static String monthJson(int year, int month, DayOfWeek first, GridMode mode, List<CalendarCell> cells) {
        return "{\n  \"schemaVersion\": " + SCHEMA_VERSION + ",\n  \"month\": " + monthObject(year, month, first, mode, cells) + "\n}\n";
    }

    private static String monthObject(int year, int month, DayOfWeek first, GridMode mode, List<CalendarCell> cells) {
        StringBuilder json = new StringBuilder("{\"yearMonth\": ").append(quote(String.format(Locale.ROOT, "%04d-%02d", year, month)))
                .append(", \"year\": ").append(year)
                .append(", \"month\": ").append(month)
                .append(", \"lengthOfMonth\": ").append(YearMonth.of(year, month).lengthOfMonth())
                .append(", \"firstDayOfWeek\": ").append(quote(first.name()))
                .append(", \"calendarRows\": ").append(cells.size() / 7)
                .append(", \"cells\": [");
        for (int index = 0; index < cells.size(); index++) {
            if (index > 0) json.append(',');
            json.append(cellJson(cells.get(index)));
        }
        return json.append("]}").toString();
    }

    private static String cellJson(CalendarCell cell) {
        if (cell.date() == null) {
            return "{\"date\":null,\"year\":null,\"month\":null,\"dayOfMonth\":null,\"dayOfYear\":null,\"dayOfWeek\":null,\"weekOfWeekBasedYear\":null,\"weekBasedYear\":null,\"row\":" + cell.row() + ",\"column\":" + cell.column() + ",\"inRequestedMonth\":false}";
        }
        LocalDate date = cell.date();
        return "{\"date\":" + quote(date.toString()) + ",\"year\":" + date.getYear() + ",\"month\":" + date.getMonthValue()
                + ",\"dayOfMonth\":" + date.getDayOfMonth() + ",\"dayOfYear\":" + date.getDayOfYear()
                + ",\"dayOfWeek\":" + quote(date.getDayOfWeek().name()) + ",\"weekOfWeekBasedYear\":" + jsonNumber(cell.weekOfWeekBasedYear())
                + ",\"weekBasedYear\":" + jsonNumber(cell.weekBasedYear()) + ",\"row\":" + cell.row() + ",\"column\":" + cell.column()
                + ",\"inRequestedMonth\":" + cell.inRequestedMonth() + "}";
    }

    private static String csv(List<CalendarCell> cells) {
        StringBuilder result = new StringBuilder(String.join(",", CSV_HEADER)).append('\n');
        for (CalendarCell cell : cells) result.append(csvRow(cell)).append('\n');
        return result.toString();
    }

    private static String csvRow(CalendarCell cell) {
        LocalDate date = cell.date();
        return String.join(",",
                date == null ? "" : date.toString(),
                date == null ? "" : Integer.toString(date.getYear()),
                date == null ? "" : Integer.toString(date.getMonthValue()),
                date == null ? "" : Integer.toString(date.getDayOfMonth()),
                date == null ? "" : date.getDayOfWeek().name(),
                date == null ? "" : Integer.toString(date.getDayOfYear()),
                cell.weekOfWeekBasedYear() == null ? "" : cell.weekOfWeekBasedYear().toString(),
                cell.weekBasedYear() == null ? "" : cell.weekBasedYear().toString(),
                Integer.toString(cell.row()), Integer.toString(cell.column()), Boolean.toString(cell.inRequestedMonth()));
    }

    private static String renderRange(LocalDate from, LocalDate to, DayOfWeek first, List<CalendarCell> cells,
                                      Map<String, String> metadata, CalendarOutputFormat format) {
        return switch (format) {
            case JSON -> {
                StringBuilder json = new StringBuilder("{\n  \"schemaVersion\": ").append(SCHEMA_VERSION)
                        .append(",\n  \"from\": ").append(quote(from.toString())).append(",\n  \"to\": ").append(quote(to.toString()))
                        .append(",\n  \"firstDayOfWeek\": ").append(quote(first.name())).append(",\n  \"metadata\": ").append(metadataJson(metadata)).append(",\n  \"cells\": [\n");
                for (int i = 0; i < cells.size(); i++) json.append("    ").append(cellJson(cells.get(i))).append(i + 1 == cells.size() ? "\n" : ",\n");
                yield json.append("  ]\n}\n").toString();
            }
            case CSV -> csv(cells);
            case MARKDOWN -> "# Calendar range " + from + " to " + to + "\n\n" + markdownTable(first, rangeMarkdownCells(cells)) + "\n";
        };
    }

    private static List<CalendarCell> rangeMarkdownCells(List<CalendarCell> cells) {
        List<CalendarCell> grid = new ArrayList<>();
        int leading = cells.isEmpty() ? 0 : cells.get(0).column();
        for (int index = 0; index < leading; index++) grid.add(new CalendarCell(null, 0, index, false, null, null));
        grid.addAll(cells);
        while (grid.size() % 7 != 0) {
            int index = grid.size();
            grid.add(new CalendarCell(null, index / 7, index % 7, false, null, null));
        }
        return grid;
    }

    private static String markdown(String title, DayOfWeek first, List<CalendarCell> cells) {
        return "# " + title + "\n\n" + markdownTable(first, cells) + "\n";
    }

    private static String markdownTable(DayOfWeek first, List<CalendarCell> cells) {
        StringBuilder md = new StringBuilder("| ");
        for (int i = 0; i < 7; i++) md.append(first.plus(i).getDisplayName(TextStyle.SHORT, Locale.ENGLISH)).append(" | ");
        md.append("\n| --- | --- | --- | --- | --- | --- | --- |\n");
        for (int i = 0; i < cells.size(); i += 7) {
            md.append('|');
            for (int j = 0; j < 7; j++) {
                CalendarCell cell = cells.get(i + j);
                String value = cell.date() == null ? " " : Integer.toString(cell.date().getDayOfMonth()) + (cell.inRequestedMonth() ? "" : "*" );
                md.append(' ').append(value).append(" | ");
            }
            md.append('\n');
        }
        return md.toString();
    }

    private static String manifest(int year, String kind, Set<CalendarOutputFormat> formats, Map<String, String> metadata, Set<String> priorFiles) {
        List<String> files = new ArrayList<>(priorFiles);
        files.sort(String::compareTo);
        StringBuilder json = new StringBuilder("{\n  \"schemaVersion\": ").append(SCHEMA_VERSION)
                .append(",\n  \"kind\": ").append(quote(kind)).append(",\n  \"year\": ").append(year)
                .append(",\n  \"formats\": [");
        List<String> formatNames = sortedFormats(formats).stream().map(Enum::name).toList();
        for (int i = 0; i < formatNames.size(); i++) json.append(i == 0 ? "" : ", ").append(quote(formatNames.get(i)));
        json.append("],\n  \"metadata\": ").append(metadataJson(metadata)).append(",\n  \"files\": [");
        for (int i = 0; i < files.size(); i++) json.append(i == 0 ? "" : ", ").append(quote(files.get(i)));
        return json.append("]\n}\n").toString();
    }

    private static List<CalendarOutputFormat> sortedFormats(Set<CalendarOutputFormat> formats) {
        return formats.stream().sorted().toList();
    }

    private static void requireFormats(Set<CalendarOutputFormat> formats, String name) {
        if (formats == null || formats.isEmpty()) throw new IllegalArgumentException("The " + name + " must specify at least one output format: JSON, CSV, or MARKDOWN.");
    }

    private static String extension(CalendarOutputFormat format) {
        return switch (format) { case JSON -> ".json"; case CSV -> ".csv"; case MARKDOWN -> ".md"; };
    }

    private static String metadataJson(Map<String, String> metadata) {
        StringBuilder json = new StringBuilder("{");
        if (metadata != null) {
            int index = 0;
            for (Map.Entry<String, String> entry : new TreeMap<>(metadata).entrySet()) {
                if (index++ > 0) json.append(',');
                json.append(quote(entry.getKey())).append(':').append(quote(entry.getValue()));
            }
        }
        return json.append('}').toString();
    }

    private static String jsonNumber(Integer value) { return value == null ? "null" : value.toString(); }

    private static String quote(String value) {
        StringBuilder escaped = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (ch < 0x20) escaped.append(String.format(Locale.ROOT, "\\u%04x", (int) ch));
                    else escaped.append(ch);
                }
            }
        }
        return escaped.append('"').toString();
    }
}
