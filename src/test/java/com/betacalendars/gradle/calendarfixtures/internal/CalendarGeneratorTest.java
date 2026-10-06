package com.betacalendars.gradle.calendarfixtures.internal;

import com.betacalendars.gradle.calendarfixtures.model.CalendarOutputFormat;
import com.betacalendars.gradle.calendarfixtures.model.GridMode;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.time.temporal.WeekFields;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CalendarGeneratorTest {
    @Test
    void followsGregorianLeapYearRulesAcrossCenturyBoundaries() {
        assertFalse(Year.isLeap(1900));
        assertTrue(Year.isLeap(2000));
        assertTrue(Year.isLeap(2024));
        assertFalse(Year.isLeap(2027));
        assertTrue(Year.isLeap(2028));
        assertFalse(Year.isLeap(2100));
        assertTrue(Year.isLeap(2400));
        assertEquals(28, YearMonth.of(1900, 2).lengthOfMonth());
        assertEquals(29, YearMonth.of(2000, 2).lengthOfMonth());
        assertEquals(29, YearMonth.of(2024, 2).lengthOfMonth());
        assertEquals(28, YearMonth.of(2027, 2).lengthOfMonth());
        assertEquals(29, YearMonth.of(2028, 2).lengthOfMonth());
        assertEquals(28, YearMonth.of(2100, 2).lengthOfMonth());
        assertEquals(29, YearMonth.of(2400, 2).lengthOfMonth());
    }

    @Test
    void supportsEveryWeekStartAndAlignsMonthStarts() {
        for (DayOfWeek first : DayOfWeek.values()) {
            for (int month = 1; month <= 12; month++) {
                List<?> cells = CalendarGenerator.monthCells(2027, month, first, GridMode.COMPACT, true, true);
                int offset = Math.floorMod(YearMonth.of(2027, month).atDay(1).getDayOfWeek().getValue() - first.getValue(), 7);
                assertTrue(offset >= 0 && offset < 7);
                assertTrue(cells.size() % 7 == 0);
                assertTrue(cells.size() >= 28 && cells.size() <= 42);
            }
        }
    }

    @Test
    void compactAndFixedGridsHaveExpectedSizesAndEmptyOverflowCells() {
        var compact = CalendarGenerator.monthCells(2027, 2, DayOfWeek.MONDAY, GridMode.COMPACT, true, true);
        var fixed = CalendarGenerator.monthCells(2027, 2, DayOfWeek.MONDAY, GridMode.FIXED_SIX_ROWS, true, true);
        var emptyOverflow = CalendarGenerator.monthCells(2027, 2, DayOfWeek.MONDAY, GridMode.FIXED_SIX_ROWS, false, true);
        assertEquals(28, compact.size());
        assertEquals(42, fixed.size());
        assertEquals(42, emptyOverflow.size());
        assertTrue(emptyOverflow.stream().anyMatch(cell -> cell.date() == null && !cell.inRequestedMonth()));
        assertEquals(0, fixed.stream().filter(cell -> LocalDate.of(2027, 2, 1).equals(cell.date())).findFirst().orElseThrow().row());
    }

    @Test
    void computesIsoWeekYearAcrossNewYear() {
        var cells = CalendarGenerator.monthCells(2021, 1, DayOfWeek.MONDAY, GridMode.COMPACT, true, true);
        var januaryFirst = cells.stream().filter(cell -> LocalDate.of(2021, 1, 1).equals(cell.date())).findFirst().orElseThrow();
        assertEquals(53, januaryFirst.weekOfWeekBasedYear());
        assertEquals(2020, januaryFirst.weekBasedYear());
        assertEquals(2021, LocalDate.of(2021, 1, 4).get(WeekFields.ISO.weekBasedYear()));
    }

    @Test
    void outputsAreStableAndMetadataIsSortedAndEscaped() {
        var formats = EnumSet.allOf(CalendarOutputFormat.class);
        var first = CalendarGenerator.generateYear(2027, DayOfWeek.MONDAY, GridMode.FIXED_SIX_ROWS, true, true, formats,
                Map.of("z", "comma, quote\"", "a", "line\nbreak"));
        var second = CalendarGenerator.generateYear(2027, DayOfWeek.MONDAY, GridMode.FIXED_SIX_ROWS, true, true, formats,
                Map.of("a", "line\nbreak", "z", "comma, quote\""));
        assertEquals(first.keySet(), second.keySet());
        first.forEach((path, bytes) -> assertArrayEquals(bytes, second.get(path), path));
        String json = new String(first.get("year.json"), java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(json.contains("\"a\":\"line\\nbreak\",\"z\":\"comma, quote\\\"\""));
        String csv = new String(first.get("months/01.csv"), java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(csv.startsWith("date,year,month,day,dayOfWeek,dayOfYear,weekOfWeekBasedYear,weekBasedYear,row,column,inRequestedMonth\n"));
        String markdown = new String(first.get("months/01.md"), java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(markdown.startsWith("# January\n\n| Mon | Tue | Wed | Thu | Fri | Sat | Sun |"));
    }

    @Test
    void rejectsInvalidYearsAndRangesWithActionableMessages() {
        IllegalArgumentException year = assertThrows(IllegalArgumentException.class, () -> CalendarGenerator.validateYear(0, "bad"));
        assertTrue(year.getMessage().contains("between 1 and 9999"));
        IllegalArgumentException range = assertThrows(IllegalArgumentException.class, () -> CalendarGenerator.generateRange(
                LocalDate.of(2027, 2, 2), LocalDate.of(2027, 2, 1), DayOfWeek.MONDAY, true,
                EnumSet.of(CalendarOutputFormat.JSON), Map.of()));
        assertTrue(range.getMessage().contains("start date must be on or before"));
    }

    @Test
    void boundaryReportIncludesLeapAndYearEdges() {
        String report = BoundaryAnalyzer.markdown(List.of(2024, 2027), List.of("2026-12-31", "2027-01-02"));
        assertTrue(report.contains("February 29: present"));
        assertTrue(report.contains("2024-02-29"));
        assertTrue(report.contains("January 1: 2027-01-01"));
        assertTrue(report.contains("December 31: 2027-12-31"));
        assertTrue(report.contains("2027-01-01"));
    }
}
