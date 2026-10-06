package com.betacalendars.gradle.calendarfixtures.tasks;

import com.betacalendars.gradle.calendarfixtures.internal.CalendarGenerator;
import com.betacalendars.gradle.calendarfixtures.model.CalendarOutputFormat;
import com.betacalendars.gradle.calendarfixtures.model.GridMode;
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
import java.util.HashSet;

@CacheableTask
public abstract class GenerateCalendarFixtureTask extends DefaultTask {
    private final Property<String> fixtureName;
    private final Property<Integer> year;
    private final Property<DayOfWeek> firstDayOfWeek;
    private final ListProperty<CalendarOutputFormat> formats;
    private final Property<Boolean> includeOverflowDays;
    private final Property<Boolean> fixedRows;
    private final Property<Boolean> includeIsoWeekNumbers;
    private final MapProperty<String, String> metadata;
    private final DirectoryProperty outputDirectory;

    @Inject
    public GenerateCalendarFixtureTask(ObjectFactory objects) {
        fixtureName = objects.property(String.class);
        year = objects.property(Integer.class);
        firstDayOfWeek = objects.property(DayOfWeek.class);
        formats = objects.listProperty(CalendarOutputFormat.class);
        includeOverflowDays = objects.property(Boolean.class);
        fixedRows = objects.property(Boolean.class);
        includeIsoWeekNumbers = objects.property(Boolean.class);
        metadata = objects.mapProperty(String.class, String.class);
        outputDirectory = objects.directoryProperty();
    }

    @Input public Property<String> getFixtureName() { return fixtureName; }
    @Input public Property<Integer> getYear() { return year; }
    @Input public Property<DayOfWeek> getFirstDayOfWeek() { return firstDayOfWeek; }
    @Input public ListProperty<CalendarOutputFormat> getFormats() { return formats; }
    @Input public Property<Boolean> getIncludeOverflowDays() { return includeOverflowDays; }
    @Input public Property<Boolean> getFixedRows() { return fixedRows; }
    @Input public Property<Boolean> getIncludeIsoWeekNumbers() { return includeIsoWeekNumbers; }
    @Input public MapProperty<String, String> getMetadata() { return metadata; }
    @OutputDirectory public DirectoryProperty getOutputDirectory() { return outputDirectory; }

    @TaskAction
    public void generate() {
        CalendarGenerator.validateYear(year.get(), fixtureName.get());
        var mode = fixedRows.get() ? GridMode.FIXED_SIX_ROWS : GridMode.COMPACT;
        var rendered = CalendarGenerator.generateYear(year.get(), firstDayOfWeek.get(), mode,
                includeOverflowDays.get(), includeIsoWeekNumbers.get(), new HashSet<>(formats.get()), metadata.get());
        Path root = outputDirectory.get().getAsFile().toPath();
        try {
            clearDirectory(root);
            rendered.forEach((relative, bytes) -> {
                try {
                    Path file = root.resolve(relative);
                    Files.createDirectories(file.getParent());
                    Files.write(file, bytes);
                } catch (IOException ex) {
                    throw new GradleException("Could not write calendar fixture '" + fixtureName.get() + "' file '" + relative + "'.", ex);
                }
            });
            getLogger().lifecycle("Generated calendar fixture '{}' for {} in {}", fixtureName.get(), year.get(), root);
        } catch (IOException ex) {
            throw new GradleException("Could not prepare output directory for calendar fixture '" + fixtureName.get() + "'.", ex);
        }
    }

    static void clearDirectory(Path root) throws IOException {
        if (!Files.exists(root)) return;
        try (var paths = Files.walk(root)) {
            for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) Files.delete(path);
        }
    }
}
