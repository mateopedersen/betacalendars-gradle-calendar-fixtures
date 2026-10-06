package com.betacalendars.gradle.calendarfixtures;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CalendarFixturesPluginFunctionalTest {
    @TempDir Path projectDir;

    @Test
    void generatesVerifiesAndReusesConfigurationCache() throws Exception {
        write("settings.gradle", "rootProject.name = 'calendar-fixture-consumer'\n");
        write("build.gradle", """
                plugins { id 'com.betacalendars.calendar-fixtures' }
                calendarFixtures {
                    calendars {
                        calendar2027 {
                            year.set(2027)
                            firstDayOfWeek.set(java.time.DayOfWeek.MONDAY)
                            fixedRows.set(true)
                            metadata.put('purpose', 'functional test')
                        }
                    }
                    ranges {
                        releaseWindow {
                            from.set('2026-12-15')
                            to.set('2027-01-15')
                        }
                    }
                }
                """);

        BuildResult first = runner("generateCalendarFixtures", "verifyCalendarFixtures", "calendarBoundaryReport", "--configuration-cache").build();
        assertEquals(TaskOutcome.SUCCESS, first.task(":generateCalendarFixtures").getOutcome());
        assertEquals(TaskOutcome.SUCCESS, first.task(":verifyCalendarFixtures").getOutcome());
        assertTrue(first.getOutput().contains("Configuration cache entry stored."));
        Path year = projectDir.resolve("build/generated/calendar-fixtures/calendar2027/year.json");
        assertTrue(Files.readString(year).contains("\"year\": 2027"));
        try (var files = Files.list(projectDir.resolve("build/generated/calendar-fixtures/calendar2027/months"))) {
            assertEquals(36, files.count());
        }
        assertTrue(Files.exists(projectDir.resolve("build/generated/calendar-fixtures/ranges/releaseWindow/range.json")));

        BuildResult second = runner("generateCalendarFixtures", "verifyCalendarFixtures", "calendarBoundaryReport", "--configuration-cache").build();
        assertTrue(second.getOutput().contains("Configuration cache entry reused."));
        assertEquals(TaskOutcome.UP_TO_DATE, second.task(":generateCalendarFixtures").getOutcome());
    }

    @Test
    void rejectsMissingYearWithUsefulFailure() throws Exception {
        write("settings.gradle", "rootProject.name = 'invalid-calendar'\n");
        write("build.gradle", "plugins { id 'com.betacalendars.calendar-fixtures' }; calendarFixtures { calendars { broken {} } }\n");
        BuildResult result = runner("generateCalendarFixtures").buildAndFail();
        assertTrue(result.getOutput().contains("year"), result.getOutput());
    }

    @Test
    void generatedTaskIsRestorableFromBuildCache() throws Exception {
        write("settings.gradle", "rootProject.name = 'calendar-cache-consumer'\n");
        write("build.gradle", "plugins { id 'com.betacalendars.calendar-fixtures' }; calendarFixtures { calendars { year2028 { year.set(2028) } } }\n");
        BuildResult first = runner("generateYear2028CalendarFixtures", "--build-cache").build();
        assertTrue(first.task(":generateYear2028CalendarFixtures").getOutcome() == TaskOutcome.SUCCESS
                || first.task(":generateYear2028CalendarFixtures").getOutcome() == TaskOutcome.FROM_CACHE);
        Path output = projectDir.resolve("build/generated/calendar-fixtures/year2028");
        try (var paths = Files.walk(output)) {
            for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) Files.delete(path);
        }
        BuildResult second = runner("generateYear2028CalendarFixtures", "--build-cache").build();
        assertEquals(TaskOutcome.FROM_CACHE, second.task(":generateYear2028CalendarFixtures").getOutcome());
        assertTrue(Files.exists(output.resolve("year.json")));
    }

    private GradleRunner runner(String... args) {
        Map<String, String> environment = new HashMap<>(System.getenv());
        String gradleHome = System.getenv("GRADLE_USER_HOME");
        if (gradleHome != null) environment.put("GRADLE_USER_HOME", gradleHome);
        return GradleRunner.create().withProjectDir(projectDir.toFile()).withArguments(args)
                .withPluginClasspath().withEnvironment(environment).forwardOutput();
    }

    private void write(String relative, String content) throws Exception {
        Path file = projectDir.resolve(relative);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }
}
