import java.time.DayOfWeek
import com.betacalendars.gradle.calendarfixtures.model.CalendarOutputFormat

plugins {
    id("com.betacalendars.calendar-fixtures")
}

calendarFixtures {
    calendars {
        create("sampleYear") {
            year.set(2027)
            firstDayOfWeek.set(DayOfWeek.MONDAY)
            formats.set(listOf(CalendarOutputFormat.JSON, CalendarOutputFormat.CSV, CalendarOutputFormat.MARKDOWN))
            fixedRows.set(true)
            includeOverflowDays.set(true)
            metadata.put("purpose", "basic-example")
        }
    }
}
