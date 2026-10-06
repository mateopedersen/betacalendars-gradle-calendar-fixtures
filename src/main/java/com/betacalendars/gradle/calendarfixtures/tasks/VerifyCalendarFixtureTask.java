package com.betacalendars.gradle.calendarfixtures.tasks;

import com.betacalendars.gradle.calendarfixtures.internal.CalendarGenerator;
import com.betacalendars.gradle.calendarfixtures.model.CalendarOutputFormat;
import com.betacalendars.gradle.calendarfixtures.model.GridMode;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@CacheableTask
public abstract class VerifyCalendarFixtureTask extends DefaultTask {
    private final Property<String> fixtureName;
    private final Property<Integer> year;
    private final Property<DayOfWeek> firstDayOfWeek;
    private final ListProperty<CalendarOutputFormat> formats;
    private final Property<Boolean> includeOverflowDays;
    private final Property<Boolean> fixedRows;
    private final Property<Boolean> includeIsoWeekNumbers;
    private final MapProperty<String, String> metadata;
    private final DirectoryProperty generatedDirectory;
    private final RegularFileProperty reportFile;

    @Inject
    public VerifyCalendarFixtureTask(ObjectFactory objects) {
        fixtureName = objects.property(String.class);
        year = objects.property(Integer.class);
        firstDayOfWeek = objects.property(DayOfWeek.class);
        formats = objects.listProperty(CalendarOutputFormat.class);
        includeOverflowDays = objects.property(Boolean.class);
        fixedRows = objects.property(Boolean.class);
        includeIsoWeekNumbers = objects.property(Boolean.class);
        metadata = objects.mapProperty(String.class, String.class);
        generatedDirectory = objects.directoryProperty();
        reportFile = objects.fileProperty();
    }

    @Input public Property<String> getFixtureName() { return fixtureName; }
    @Input public Property<Integer> getYear() { return year; }
    @Input public Property<DayOfWeek> getFirstDayOfWeek() { return firstDayOfWeek; }
    @Input public ListProperty<CalendarOutputFormat> getFormats() { return formats; }
    @Input public Property<Boolean> getIncludeOverflowDays() { return includeOverflowDays; }
    @Input public Property<Boolean> getFixedRows() { return fixedRows; }
    @Input public Property<Boolean> getIncludeIsoWeekNumbers() { return includeIsoWeekNumbers; }
    @Input public MapProperty<String, String> getMetadata() { return metadata; }
    @InputDirectory @PathSensitive(PathSensitivity.RELATIVE) public DirectoryProperty getGeneratedDirectory() { return generatedDirectory; }
    @OutputFile public RegularFileProperty getReportFile() { return reportFile; }

    @TaskAction
    public void verify() {
        var mode = fixedRows.get() ? GridMode.FIXED_SIX_ROWS : GridMode.COMPACT;
        Map<String, byte[]> expected = CalendarGenerator.generateYear(year.get(), firstDayOfWeek.get(), mode,
                includeOverflowDays.get(), includeIsoWeekNumbers.get(), new HashSet<>(formats.get()), metadata.get());
        Path root = generatedDirectory.get().getAsFile().toPath();
        try {
            Map<String, byte[]> actual = readFiles(root);
            if (!actual.keySet().equals(expected.keySet())) {
                Set<String> missing = new HashSet<>(expected.keySet()); missing.removeAll(actual.keySet());
                Set<String> unexpected = new HashSet<>(actual.keySet()); unexpected.removeAll(expected.keySet());
                throw new GradleException("Calendar fixture '" + fixtureName.get() + "' has an unexpected file set. Missing: " + missing + "; unexpected: " + unexpected + ". Run generateCalendarFixtures to regenerate it.");
            }
            for (String file : expected.keySet()) {
                if (!java.util.Arrays.equals(expected.get(file), actual.get(file))) {
                    throw new GradleException("Calendar fixture '" + fixtureName.get() + "' file '" + file + "' is stale or invalid. Run its generate task to regenerate it.");
                }
            }
            Path report = reportFile.get().getAsFile().toPath();
            Files.createDirectories(report.getParent());
            Files.writeString(report, "PASS: '" + fixtureName.get() + "' has " + expected.size() + " deterministic files for " + year.get() + ".\n", StandardCharsets.UTF_8);
            getLogger().lifecycle("Verified calendar fixture '{}' for {} ({} files)", fixtureName.get(), year.get(), expected.size());
        } catch (IOException ex) {
            throw new GradleException("Could not read or report calendar fixture '" + fixtureName.get() + "'.", ex);
        }
    }

    private static Map<String, byte[]> readFiles(Path root) throws IOException {
        if (!Files.isDirectory(root)) throw new GradleException("Generated calendar fixture directory does not exist: " + root + ". Run generateCalendarFixtures first.");
        Map<String, byte[]> files = new HashMap<>();
        try (var paths = Files.walk(root)) {
            for (Path file : paths.filter(Files::isRegularFile).toList()) {
                files.put(root.relativize(file).toString().replace(file.getFileSystem().getSeparator(), "/"), Files.readAllBytes(file));
            }
        }
        return files;
    }
}
