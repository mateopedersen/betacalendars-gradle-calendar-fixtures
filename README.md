# Beta Calendars Calendar Fixtures Gradle Plugin

[![CI](https://github.com/mateopedersen/betacalendars-gradle-calendar-fixtures/actions/workflows/ci.yml/badge.svg)](https://github.com/mateopedersen/betacalendars-gradle-calendar-fixtures/actions/workflows/ci.yml)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/com.betacalendars.calendar-fixtures)](https://plugins.gradle.org/plugin/com.betacalendars.calendar-fixtures)
[![Apache-2.0](https://img.shields.io/badge/license-Apache--2.0-blue.svg)](LICENSE)

Generate deterministic calendar datasets during a Gradle build. The plugin uses `java.time`, works offline once Gradle has resolved its build dependencies, and makes no network calls when its tasks execute.

It is designed for date-picker tests, calendar UI snapshots, documentation, static resources, scheduling edge cases, and sample datasets. Its calendar model is independent of Beta Calendars services.

## Requirements

- Gradle 8.10.2 or newer (the test suite currently verifies Gradle 8.10.2 and 9.8.0)
- JDK 17 or newer to run Gradle

The plugin implementation targets Java 17. It uses only public Gradle APIs. Configuration Cache compatibility is tested. Build Cache support is tested. Isolated Projects compatibility is not declared.

## Apply the plugin

```kotlin
plugins {
    id("com.betacalendars.calendar-fixtures") version "1.0.0"
}
```

## Generate a year

```kotlin
import java.time.DayOfWeek
import com.betacalendars.gradle.calendarfixtures.model.CalendarOutputFormat

calendarFixtures {
    calendars {
        create("calendar2027") {
            year.set(2027)
            firstDayOfWeek.set(DayOfWeek.MONDAY)
            formats.set(listOf(
                CalendarOutputFormat.JSON,
                CalendarOutputFormat.CSV,
                CalendarOutputFormat.MARKDOWN
            ))
            includeOverflowDays.set(true)
            fixedRows.set(true)
            includeIsoWeekNumbers.set(true)
            metadata.put("purpose", "ui-regression-tests")
        }
    }
}
```

Run `./gradlew generateCalendarFixtures` to generate every configured definition. Each definition also has its own task; the example above registers `generateCalendar2027CalendarFixtures`. Run `./gradlew verifyCalendarFixtures` to regenerate and compare every fixture with its expected deterministic bytes. Use the matching `verifyCalendar2027CalendarFixtures` task for one definition.

The default output directory is `build/generated/calendar-fixtures/`. A year definition named `calendar2027` writes to `calendar2027/`, including `year.json`, `year.csv`, `year.md`, `months/01.*` through `months/12.*`, `boundaries.json`, and `manifest.json`. Set `outputDirectory` on the extension or an individual definition to change the location.

## Generate an inclusive date range

```kotlin
import java.time.DayOfWeek
calendarFixtures {
    ranges {
        create("releaseWindow") {
            from.set("2026-12-15")
            to.set("2027-01-15")
            firstDayOfWeek.set(DayOfWeek.MONDAY)
        }
    }
}
```

Run `./gradlew generateCalendarFixtures` or the per-range task `./gradlew generateReleaseWindowRangeFixtures`. The default output is `build/generated/calendar-fixtures/ranges/releaseWindow/`. Range endpoints are inclusive and configured as ISO-8601 strings (YYYY-MM-DD); reversed or overlong ranges fail with an actionable message.

## Groovy DSL

```groovy
import java.time.DayOfWeek
import com.betacalendars.gradle.calendarfixtures.model.CalendarOutputFormat

plugins {
    id 'com.betacalendars.calendar-fixtures' version '1.0.0'
}

calendarFixtures {
    calendars {
        create('us2027') {
            year.set(2027)
            firstDayOfWeek.set(DayOfWeek.SUNDAY)
            formats.set([CalendarOutputFormat.JSON, CalendarOutputFormat.CSV])
            fixedRows.set(false)
            includeOverflowDays.set(true)
            includeIsoWeekNumbers.set(true)
        }
    }
}
```

## Formats and grid behavior

- **JSON** is UTF-8 with LF line endings, stable key ordering, `schemaVersion: 1`, and explicit `null` fields for blank cells.
- **CSV** has a fixed header: `date,year,month,day,dayOfWeek,dayOfYear,weekOfWeekBasedYear,weekBasedYear,row,column,inRequestedMonth`. Blank cells have empty date-derived columns and retain their row/column positions.
- **Markdown** renders Monday-, Sunday-, or any other selected weekday-first tables. Overflow dates have an asterisk; blank cells stay empty.
- **COMPACT** grids use the minimum number of rows needed. **FIXED_SIX_ROWS** grids always contain 42 cells per month. The DSL's `fixedRows` flag selects between them.
- With `includeOverflowDays = false`, cells outside the selected month remain in the grid as blank cells.
- Week number fields use ISO-8601 week rules. `firstDayOfWeek` controls display-grid alignment and does not change ISO week numbering.

A generated manifest lists the deterministic files, format names, schema version, and sorted string metadata. Generated data contains no timestamp, machine name, user name, absolute path, or random identifier.

See [Output schema and compatibility policy](docs/output-schema.md) for the field definitions and stability guarantees.

## Verification and boundaries

`verifyCalendarFixtures` checks the expected file set and exact bytes generated from the configured year, grid, formats, and metadata. This validates date continuity, Gregorian leap days, weekday placement, grid size, ISO week/year values, and manifest consistency against the same `java.time` model.

`calendarBoundaryReport` writes `build/reports/calendar-fixtures/calendar-boundaries.md` with January 1, December 31, February 29 when present, ISO week-year transitions, and each month's first/last dates and weekdays. It also includes configured date ranges.

## Cache behavior

Generation and verification tasks declare their Gradle inputs and outputs and are `@CacheableTask`. TestKit checks Build Cache restoration and executes the same consumer build twice with `--configuration-cache`, confirming that the configuration entry is stored and reused.

## Examples

Runnable consumer projects are in [`examples/basic`](examples/basic), [`examples/kotlin-dsl`](examples/kotlin-dsl), [`examples/groovy-dsl`](examples/groovy-dsl), and [`examples/fixture-verification`](examples/fixture-verification). From the repository root, run each with the wrapper and its project directory, for example `./gradlew -p examples/basic verifyCalendarFixtures`.

The [January 2027 sample](examples/fixture-verification/generated/january-2027.md) shows a real Monday-first, six-row month grid. The example build generates its JSON, CSV, and Markdown files.

## Contributing and security

See [CONTRIBUTING.md](CONTRIBUTING.md), [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md), and [SECURITY.md](SECURITY.md). Please report vulnerabilities privately as described in the security policy.

## License and attribution

The plugin is licensed under [Apache License 2.0](LICENSE). Beta Calendars maintains this generic build-time generator. Calendar layouts are computed locally with `java.time`; there is no runtime dependency on or connection to [Beta Calendars](https://www.betacalendars.com/).
