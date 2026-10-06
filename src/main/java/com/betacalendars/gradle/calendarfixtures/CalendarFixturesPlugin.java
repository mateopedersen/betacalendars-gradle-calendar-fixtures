package com.betacalendars.gradle.calendarfixtures;

import com.betacalendars.gradle.calendarfixtures.tasks.CalendarBoundaryReportTask;
import com.betacalendars.gradle.calendarfixtures.tasks.GenerateCalendarFixtureTask;
import com.betacalendars.gradle.calendarfixtures.tasks.GenerateDateRangeTask;
import com.betacalendars.gradle.calendarfixtures.tasks.VerifyCalendarFixtureTask;
import com.betacalendars.gradle.calendarfixtures.tasks.VerifyDateRangeTask;
import org.gradle.api.DefaultTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.TaskProvider;

import java.util.Locale;
import java.util.regex.Pattern;

/** Registers cacheable calendar fixture generation and verification tasks. */
public final class CalendarFixturesPlugin implements Plugin<Project> {
    private static final Pattern NAME_PATTERN = Pattern.compile("[A-Za-z][A-Za-z0-9_-]*");

    @Override
    public void apply(Project project) {
        CalendarFixturesExtension extension = project.getExtensions().create(
                "calendarFixtures", CalendarFixturesExtension.class);

        TaskProvider<DefaultTask> generateAll = project.getTasks().register("generateCalendarFixtures", DefaultTask.class, task -> {
            task.setGroup("calendar fixtures");
            task.setDescription("Generates all configured calendar and date range fixtures.");
        });
        TaskProvider<DefaultTask> verifyAll = project.getTasks().register("verifyCalendarFixtures", DefaultTask.class, task -> {
            task.setGroup("verification");
            task.setDescription("Generates and verifies all configured calendar and date range fixtures.");
        });
        TaskProvider<CalendarBoundaryReportTask> boundaryReport = project.getTasks().register("calendarBoundaryReport", CalendarBoundaryReportTask.class, task -> {
            task.setGroup("calendar fixtures");
            task.setDescription("Writes a human-readable report for configured calendar and date range boundaries.");
            task.getYears().convention(java.util.List.of());
            task.getRanges().convention(java.util.List.of());
            task.getReportFile().convention(project.getLayout().getBuildDirectory().file("reports/calendar-fixtures/calendar-boundaries.md"));
        });

        extension.getCalendars().configureEach(definition -> {
            validateName(definition.getName(), "calendar");
            definition.getOutputDirectory().convention(extension.getOutputDirectory().dir(definition.getName()));
            String suffix = taskSuffix(definition.getName()) + "Calendar";
            TaskProvider<GenerateCalendarFixtureTask> generate = project.getTasks().register("generate" + suffix + "Fixtures", GenerateCalendarFixtureTask.class, task -> {
                task.setGroup("calendar fixtures");
                task.setDescription("Generates calendar fixture '" + definition.getName() + "'.");
                task.getFixtureName().set(definition.getName());
                task.getYear().set(definition.getYear());
                task.getFirstDayOfWeek().set(definition.getFirstDayOfWeek());
                task.getFormats().set(definition.getFormats());
                task.getIncludeOverflowDays().set(definition.getIncludeOverflowDays());
                task.getFixedRows().set(definition.getFixedRows());
                task.getIncludeIsoWeekNumbers().set(definition.getIncludeIsoWeekNumbers());
                task.getMetadata().set(definition.getMetadata());
                task.getOutputDirectory().set(definition.getOutputDirectory());
            });
            TaskProvider<VerifyCalendarFixtureTask> verify = project.getTasks().register("verify" + suffix + "Fixtures", VerifyCalendarFixtureTask.class, task -> {
                task.setGroup("verification");
                task.setDescription("Verifies generated calendar fixture '" + definition.getName() + "'.");
                task.getFixtureName().set(definition.getName());
                task.getYear().set(definition.getYear());
                task.getFirstDayOfWeek().set(definition.getFirstDayOfWeek());
                task.getFormats().set(definition.getFormats());
                task.getIncludeOverflowDays().set(definition.getIncludeOverflowDays());
                task.getFixedRows().set(definition.getFixedRows());
                task.getIncludeIsoWeekNumbers().set(definition.getIncludeIsoWeekNumbers());
                task.getMetadata().set(definition.getMetadata());
                task.getGeneratedDirectory().set(definition.getOutputDirectory());
                task.getReportFile().convention(project.getLayout().getBuildDirectory().file("reports/calendar-fixtures/" + definition.getName() + ".verification.txt"));
                task.dependsOn(generate);
            });
            generateAll.configure(task -> task.dependsOn(generate));
            verifyAll.configure(task -> task.dependsOn(verify));
            boundaryReport.configure(task -> task.getYears().add(definition.getYear()));
        });

        extension.getRanges().configureEach(definition -> {
            validateName(definition.getName(), "range");
            definition.getOutputDirectory().convention(extension.getOutputDirectory().dir("ranges/" + definition.getName()));
            String suffix = taskSuffix(definition.getName()) + "Range";
            TaskProvider<GenerateDateRangeTask> generate = project.getTasks().register("generate" + suffix + "Fixtures", GenerateDateRangeTask.class, task -> {
                task.setGroup("calendar fixtures");
                task.setDescription("Generates date range fixture '" + definition.getName() + "'.");
                task.getFixtureName().set(definition.getName());
                task.getFrom().set(definition.getFrom());
                task.getTo().set(definition.getTo());
                task.getFirstDayOfWeek().set(definition.getFirstDayOfWeek());
                task.getFormats().set(definition.getFormats());
                task.getIncludeIsoWeekNumbers().set(definition.getIncludeIsoWeekNumbers());
                task.getMetadata().set(definition.getMetadata());
                task.getOutputDirectory().set(definition.getOutputDirectory());
            });
            TaskProvider<VerifyDateRangeTask> verify = project.getTasks().register("verify" + suffix + "Fixtures", VerifyDateRangeTask.class, task -> {
                task.setGroup("verification");
                task.setDescription("Verifies generated date range fixture '" + definition.getName() + "'.");
                task.getFixtureName().set(definition.getName());
                task.getFrom().set(definition.getFrom());
                task.getTo().set(definition.getTo());
                task.getFirstDayOfWeek().set(definition.getFirstDayOfWeek());
                task.getFormats().set(definition.getFormats());
                task.getIncludeIsoWeekNumbers().set(definition.getIncludeIsoWeekNumbers());
                task.getMetadata().set(definition.getMetadata());
                task.getGeneratedDirectory().set(definition.getOutputDirectory());
                task.getReportFile().convention(project.getLayout().getBuildDirectory().file("reports/calendar-fixtures/" + definition.getName() + ".verification.txt"));
                task.dependsOn(generate);
            });
            generateAll.configure(task -> task.dependsOn(generate));
            verifyAll.configure(task -> task.dependsOn(verify));
            boundaryReport.configure(task -> {
                task.getRanges().add(definition.getFrom());
                task.getRanges().add(definition.getTo());
            });
        });
    }

    private static void validateName(String name, String kind) {
        if (!NAME_PATTERN.matcher(name).matches()) {
            throw new org.gradle.api.GradleException("Calendar fixture " + kind + " name '" + name + "' is invalid. Use a letter followed by letters, digits, hyphens, or underscores.");
        }
    }

    private static String taskSuffix(String name) {
        StringBuilder result = new StringBuilder();
        boolean upper = true;
        for (char ch : name.toCharArray()) {
            if (ch == '-' || ch == '_') { upper = true; continue; }
            result.append(upper ? Character.toUpperCase(ch) : ch);
            upper = false;
        }
        return result.toString();
    }
}
