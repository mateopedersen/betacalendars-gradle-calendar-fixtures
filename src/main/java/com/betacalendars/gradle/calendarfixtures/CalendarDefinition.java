package com.betacalendars.gradle.calendarfixtures;

import com.betacalendars.gradle.calendarfixtures.model.CalendarOutputFormat;
import org.gradle.api.Named;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;

import javax.inject.Inject;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;

/** A named year calendar fixture definition in the calendarFixtures DSL. */
public class CalendarDefinition implements Named {
    private final String name;
    private final Property<Integer> year;
    private final Property<DayOfWeek> firstDayOfWeek;
    private final ListProperty<CalendarOutputFormat> formats;
    private final Property<Boolean> includeOverflowDays;
    private final Property<Boolean> fixedRows;
    private final Property<Boolean> includeIsoWeekNumbers;
    private final MapProperty<String, String> metadata;
    private final DirectoryProperty outputDirectory;

    @Inject
    public CalendarDefinition(String name, ObjectFactory objects) {
        this.name = name;
        year = objects.property(Integer.class);
        firstDayOfWeek = objects.property(DayOfWeek.class).convention(DayOfWeek.MONDAY);
        formats = objects.listProperty(CalendarOutputFormat.class).convention(List.of(CalendarOutputFormat.JSON, CalendarOutputFormat.CSV, CalendarOutputFormat.MARKDOWN));
        includeOverflowDays = objects.property(Boolean.class).convention(true);
        fixedRows = objects.property(Boolean.class).convention(false);
        includeIsoWeekNumbers = objects.property(Boolean.class).convention(true);
        metadata = objects.mapProperty(String.class, String.class).convention(Map.of());
        outputDirectory = objects.directoryProperty();
    }

    @Override public String getName() { return name; }
    public Property<Integer> getYear() { return year; }
    public Property<DayOfWeek> getFirstDayOfWeek() { return firstDayOfWeek; }
    public ListProperty<CalendarOutputFormat> getFormats() { return formats; }
    public Property<Boolean> getIncludeOverflowDays() { return includeOverflowDays; }
    public Property<Boolean> getFixedRows() { return fixedRows; }
    public Property<Boolean> getIncludeIsoWeekNumbers() { return includeIsoWeekNumbers; }
    public MapProperty<String, String> getMetadata() { return metadata; }
    public DirectoryProperty getOutputDirectory() { return outputDirectory; }
}
