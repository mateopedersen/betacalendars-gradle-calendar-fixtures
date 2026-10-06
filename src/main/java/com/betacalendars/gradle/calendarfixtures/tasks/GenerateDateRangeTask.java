package com.betacalendars.gradle.calendarfixtures.tasks;

import com.betacalendars.gradle.calendarfixtures.internal.CalendarGenerator;
import com.betacalendars.gradle.calendarfixtures.model.CalendarOutputFormat;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashSet;

@CacheableTask
public abstract class GenerateDateRangeTask extends DefaultTask {
    private final Property<String> fixtureName;
    private final Property<String> from;
    private final Property<String> to;
    private final Property<DayOfWeek> firstDayOfWeek;
    private final ListProperty<CalendarOutputFormat> formats;
    private final Property<Boolean> includeIsoWeekNumbers;
    private final MapProperty<String, String> metadata;
    private final DirectoryProperty outputDirectory;

    @Inject
    public GenerateDateRangeTask(ObjectFactory objects) {
        fixtureName = objects.property(String.class);
        from = objects.property(String.class);
        to = objects.property(String.class);
        firstDayOfWeek = objects.property(DayOfWeek.class);
        formats = objects.listProperty(CalendarOutputFormat.class);
        includeIsoWeekNumbers = objects.property(Boolean.class);
        metadata = objects.mapProperty(String.class, String.class);
        outputDirectory = objects.directoryProperty();
    }

    @Input public Property<String> getFixtureName() { return fixtureName; }
    @Input public Property<String> getFrom() { return from; }
    @Input public Property<String> getTo() { return to; }
    @Input public Property<DayOfWeek> getFirstDayOfWeek() { return firstDayOfWeek; }
    @Input public ListProperty<CalendarOutputFormat> getFormats() { return formats; }
    @Input public Property<Boolean> getIncludeIsoWeekNumbers() { return includeIsoWeekNumbers; }
    @Input public MapProperty<String, String> getMetadata() { return metadata; }
    @OutputDirectory public DirectoryProperty getOutputDirectory() { return outputDirectory; }

    @TaskAction
    public void generate() {
        LocalDate start = parseDate(from.get(), "from");
        LocalDate end = parseDate(to.get(), "to");
        var rendered = CalendarGenerator.generateRange(start, end, firstDayOfWeek.get(), includeIsoWeekNumbers.get(), new HashSet<>(formats.get()), metadata.get());
        Path root = outputDirectory.get().getAsFile().toPath();
        try {
            GenerateCalendarFixtureTask.clearDirectory(root);
            rendered.forEach((relative, bytes) -> {
                try {
                    Path file = root.resolve(relative);
                    Files.createDirectories(file.getParent());
                    Files.write(file, bytes);
                } catch (IOException ex) {
                    throw new GradleException("Could not write date range fixture '" + fixtureName.get() + "' file '" + relative + "'.", ex);
                }
            });
            getLogger().lifecycle("Generated date range fixture '{}' for {} through {} in {}", fixtureName.get(), start, end, root);
        } catch (IOException ex) {
            throw new GradleException("Could not prepare output directory for date range fixture '" + fixtureName.get() + "'.", ex);
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
