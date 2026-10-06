import java.time.DayOfWeek
import com.betacalendars.gradle.calendarfixtures.model.CalendarOutputFormat

plugins {
    id("com.betacalendars.calendar-fixtures")
}

calendarFixtures {
    calendars {
        create("isoYear") {
            year.set(2028)
            firstDayOfWeek.set(DayOfWeek.MONDAY)
            fixedRows.set(false)
            includeIsoWeekNumbers.set(true)
            formats.set(listOf(CalendarOutputFormat.JSON, CalendarOutputFormat.MARKDOWN))
        }
    }
    ranges {
        create("newYearWindow") {
            from.set("2027-12-29")
            to.set("2028-01-05")
            firstDayOfWeek.set(DayOfWeek.MONDAY)
        }
    }
}
