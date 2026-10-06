# Output schema version 1

All text files are UTF-8 with LF line endings. JSON keys are emitted in a fixed order, metadata keys and manifest file paths are sorted, and output never contains build timestamps or machine-specific paths.

## Year JSON

`year.json` contains `schemaVersion`, `year`, `leapYear`, `firstDayOfWeek`, `gridMode`, `includeOverflowDays`, `includeIsoWeekNumbers`, `metadata`, and `months`.

Each month has `yearMonth` (`YYYY-MM`), `year`, `month`, `lengthOfMonth`, `firstDayOfWeek`, `calendarRows`, and `cells`. Each cell has `date` (ISO `YYYY-MM-DD`, or `null` for a blank), `year`, `month`, `dayOfMonth`, `dayOfYear`, `dayOfWeek`, `weekOfWeekBasedYear`, `weekBasedYear`, `row`, `column`, and `inRequestedMonth`. Date-derived values are `null` when the cell is blank or ISO week numbers are disabled.

Individual month JSON files wrap the same month object under `month`, alongside `schemaVersion`.

## Range JSON

Range JSON has `schemaVersion`, inclusive `from` and `to` dates, `firstDayOfWeek`, sorted string `metadata`, and `cells`. Cells include the same date, ISO week, and grid-position fields as year data. Range rows are aligned to the selected first weekday; only requested dates appear in range JSON and CSV.

## CSV and Markdown

CSV fields and order are stable and documented in the root README. Year CSV adds a final `month` field. CSV dates use ISO format, weekdays use `DayOfWeek` enum names, booleans use `true`/`false`, and blank cells have empty date-derived values.

Markdown tables use the chosen weekday order. Month grids display overflow dates with `*`; range grids are padded with empty cells to complete their final visible row. Markdown is for readable fixtures and documentation, not a parsing API.

## Boundaries and manifest

`boundaries.json` is a compact diagnostic dataset of year/month edges and ISO week-year transitions. A range boundary file lists its endpoints and transitions contained in the inclusive range. `manifest.json` records `schemaVersion`, dataset `kind`, year anchor, sorted formats, metadata, and sorted generated file paths.

## Compatibility policy

Schema version 1 is the default contract for plugin 1.x. Adding optional fields may occur only with a documented minor release; removing or changing field meaning, date semantics, blank-cell representation, CSV column order, or deterministic rendering requires a schema version increment and a migration note. Consumers should check `schemaVersion` before relying on a schema.
