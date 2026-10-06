package com.betacalendars.gradle.calendarfixtures.tasks;

import com.betacalendars.gradle.calendarfixtures.internal.BoundaryAnalyzer;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@CacheableTask
public abstract class CalendarBoundaryReportTask extends DefaultTask {
    private final ListProperty<Integer> years;
    private final ListProperty<String> ranges;
    private final RegularFileProperty reportFile;

    @Inject
    public CalendarBoundaryReportTask(ObjectFactory objects) {
        years = objects.listProperty(Integer.class);
        ranges = objects.listProperty(String.class);
        reportFile = objects.fileProperty();
    }

    @Input public ListProperty<Integer> getYears() { return years; }
    /** Inclusive range endpoints are supplied as alternating ISO date strings. */
    @Input public ListProperty<String> getRanges() { return ranges; }
    @OutputFile public RegularFileProperty getReportFile() { return reportFile; }

    @TaskAction
    public void report() {
        Path output = reportFile.get().getAsFile().toPath();
        try {
            Files.createDirectories(output.getParent());
            Files.writeString(output, BoundaryAnalyzer.markdown(years.get(), ranges.get()), StandardCharsets.UTF_8);
            getLogger().lifecycle("Wrote calendar boundary report to {}", output);
        } catch (IOException ex) {
            throw new org.gradle.api.GradleException("Could not write calendar boundary report to '" + output + "'.", ex);
        }
    }
}
