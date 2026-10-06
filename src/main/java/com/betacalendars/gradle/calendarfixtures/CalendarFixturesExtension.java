package com.betacalendars.gradle.calendarfixtures;

import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.file.ProjectLayout;

import javax.inject.Inject;

/** DSL extension for deterministic calendar fixture generation. */
public class CalendarFixturesExtension {
    private final NamedDomainObjectContainer<CalendarDefinition> calendars;
    private final NamedDomainObjectContainer<RangeDefinition> ranges;
    private final DirectoryProperty outputDirectory;

    @Inject
    public CalendarFixturesExtension(ObjectFactory objects, ProjectLayout layout) {
        calendars = objects.domainObjectContainer(CalendarDefinition.class,
                name -> objects.newInstance(CalendarDefinition.class, name));
        ranges = objects.domainObjectContainer(RangeDefinition.class,
                name -> objects.newInstance(RangeDefinition.class, name));
        outputDirectory = objects.directoryProperty().convention(layout.getBuildDirectory().dir("generated/calendar-fixtures"));
    }

    public NamedDomainObjectContainer<CalendarDefinition> getCalendars() { return calendars; }
    public NamedDomainObjectContainer<RangeDefinition> getRanges() { return ranges; }
    public DirectoryProperty getOutputDirectory() { return outputDirectory; }
}
