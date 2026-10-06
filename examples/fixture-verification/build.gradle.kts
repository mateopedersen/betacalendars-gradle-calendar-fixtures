import java.time.DayOfWeek
import com.betacalendars.gradle.calendarfixtures.model.CalendarOutputFormat

plugins {
    id("com.betacalendars.calendar-fixtures")
}

calendarFixtures {
    calendars {
        create("january2027Example") {
            year.set(2027)
            firstDayOfWeek.set(DayOfWeek.MONDAY)
            fixedRows.set(true)
            includeOverflowDays.set(true)
            formats.set(listOf(CalendarOutputFormat.JSON, CalendarOutputFormat.CSV, CalendarOutputFormat.MARKDOWN))
            metadata.put("sample", "January 2027; Monday-first; fixed six rows")
        }
    }
}
