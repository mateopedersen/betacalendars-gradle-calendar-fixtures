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

/** A named, inclusive date range fixture definition. */
public class RangeDefinition implements Named {
    private final String name;
    private final Property<String> from;
    private final Property<String> to;
    private final Property<DayOfWeek> firstDayOfWeek;
    private final ListProperty<CalendarOutputFormat> formats;
    private final Property<Boolean> includeIsoWeekNumbers;
    private final MapProperty<String, String> metadata;
    private final DirectoryProperty outputDirectory;

    @Inject
    public RangeDefinition(String name, ObjectFactory objects) {
        this.name = name;
        from = objects.property(String.class);
        to = objects.property(String.class);
        firstDayOfWeek = objects.property(DayOfWeek.class).convention(DayOfWeek.MONDAY);
        formats = objects.listProperty(CalendarOutputFormat.class).convention(List.of(CalendarOutputFormat.JSON, CalendarOutputFormat.CSV, CalendarOutputFormat.MARKDOWN));
        includeIsoWeekNumbers = objects.property(Boolean.class).convention(true);
        metadata = objects.mapProperty(String.class, String.class).convention(Map.of());
        outputDirectory = objects.directoryProperty();
    }

    @Override public String getName() { return name; }
    public Property<String> getFrom() { return from; }
    public Property<String> getTo() { return to; }
    public Property<DayOfWeek> getFirstDayOfWeek() { return firstDayOfWeek; }
    public ListProperty<CalendarOutputFormat> getFormats() { return formats; }
    public Property<Boolean> getIncludeIsoWeekNumbers() { return includeIsoWeekNumbers; }
    public MapProperty<String, String> getMetadata() { return metadata; }
    public DirectoryProperty getOutputDirectory() { return outputDirectory; }
}
