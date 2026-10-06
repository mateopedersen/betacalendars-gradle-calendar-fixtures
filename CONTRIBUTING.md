# Contributing

Thanks for helping improve Calendar Fixtures. Please open an issue for larger API or schema changes before submitting a pull request.

## Development setup

- JDK 17 or newer
- The Gradle Wrapper in this repository

Run `./gradlew check validatePlugins publishPlugins --validate-only` to execute unit tests, TestKit tests, Gradle task validation, and metadata validation. Never include Portal credentials in a patch or build file. Generated example outputs can be refreshed with `./gradlew -p examples/fixture-verification generateCalendarFixtures`.

## Pull requests

Keep changes focused, add tests for calendar edge cases, update the output schema and changelog when behavior changes, and verify documented examples. Do not add network access, telemetry, analytics, or runtime services to the plugin.
