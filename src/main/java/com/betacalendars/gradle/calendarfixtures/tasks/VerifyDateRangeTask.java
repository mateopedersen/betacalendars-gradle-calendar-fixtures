package com.betacalendars.gradle.calendarfixtures.tasks;

import com.betacalendars.gradle.calendarfixtures.internal.CalendarGenerator;
import com.betacalendars.gradle.calendarfixtures.model.CalendarOutputFormat;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;

@CacheableTask
public abstract class VerifyDateRangeTask extends DefaultTask {
    private final Property<String> fixtureName;
    private final Property<String> from;
    private final Property<String> to;
    private final Property<DayOfWeek> firstDayOfWeek;
    private final ListProperty<CalendarOutputFormat> formats;
    private final Property<Boolean> includeIsoWeekNumbers;
    private final MapProperty<String, String> metadata;
    private final DirectoryProperty generatedDirectory;
    private final RegularFileProperty reportFile;

    @Inject
    public VerifyDateRangeTask(ObjectFactory objects) {
        fixtureName = objects.property(String.class);
        from = objects.property(String.class);
        to = objects.property(String.class);
        firstDayOfWeek = objects.property(DayOfWeek.class);
        formats = objects.listProperty(CalendarOutputFormat.class);
        includeIsoWeekNumbers = objects.property(Boolean.class);
        metadata = objects.mapProperty(String.class, String.class);
        generatedDirectory = objects.directoryProperty();
        reportFile = objects.fileProperty();
    }

    @Input public Property<String> getFixtureName() { return fixtureName; }
    @Input public Property<String> getFrom() { return from; }
    @Input public Property<String> getTo() { return to; }
    @Input public Property<DayOfWeek> getFirstDayOfWeek() { return firstDayOfWeek; }
    @Input public ListProperty<CalendarOutputFormat> getFormats() { return formats; }
    @Input public Property<Boolean> getIncludeIsoWeekNumbers() { return includeIsoWeekNumbers; }
    @Input public MapProperty<String, String> getMetadata() { return metadata; }
    @InputDirectory @PathSensitive(PathSensitivity.RELATIVE) public DirectoryProperty getGeneratedDirectory() { return generatedDirectory; }
    @OutputFile public RegularFileProperty getReportFile() { return reportFile; }

    @TaskAction
    public void verify() {
        LocalDate start = parseDate(from.get(), "from");
        LocalDate end = parseDate(to.get(), "to");
        var expected = CalendarGenerator.generateRange(start, end, firstDayOfWeek.get(), includeIsoWeekNumbers.get(), new HashSet<>(formats.get()), metadata.get());
        Path root = generatedDirectory.get().getAsFile().toPath();
        try {
            if (!Files.isDirectory(root)) throw new GradleException("Generated date range fixture directory does not exist: " + root + ". Run generateCalendarFixtures first.");
            java.util.Map<String, byte[]> actual = new java.util.HashMap<>();
            try (var paths = Files.walk(root)) {
                for (Path file : paths.filter(Files::isRegularFile).toList()) actual.put(root.relativize(file).toString().replace(file.getFileSystem().getSeparator(), "/"), Files.readAllBytes(file));
            }
            if (!actual.keySet().equals(expected.keySet())) throw new GradleException("Date range fixture '" + fixtureName.get() + "' has missing or unexpected files. Run generateCalendarFixtures to regenerate it.");
            for (String file : expected.keySet()) if (!Arrays.equals(expected.get(file), actual.get(file))) throw new GradleException("Date range fixture '" + fixtureName.get() + "' file '" + file + "' is stale or invalid. Run its generate task to regenerate it.");
            Path report = reportFile.get().getAsFile().toPath();
            Files.createDirectories(report.getParent());
            Files.writeString(report, "PASS: '" + fixtureName.get() + "' has " + expected.size() + " deterministic files for " + start + " through " + end + ".\n", StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new GradleException("Could not read or report date range fixture '" + fixtureName.get() + "'.", ex);
        }
    }

    private LocalDate parseDate(String value, String field) {
        try {
            return LocalDate.parse(value);
        } catch (RuntimeException ex) {
            throw new GradleException("Date range fixture '" + fixtureName.get() + "' has an invalid " + field + " date '" + value + "'. Use ISO-8601 format YYYY-MM-DD.", ex);
        }
    }
}
