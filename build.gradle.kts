import org.gradle.plugin.compatibility.compatibility

plugins {
    `java-gradle-plugin`
    id("com.gradle.plugin-publish") version "2.2.1"
}

group = "com.betacalendars"
version = "1.0.0"
description = "Generate deterministic calendar fixtures and boundary reports for Gradle builds."

repositories { mavenCentral() }

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(17)) }
    withSourcesJar()
    withJavadocJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)
    options.encoding = "UTF-8"
}

val functionalTest = sourceSets.create("functionalTest")

configurations[functionalTest.implementationConfigurationName].extendsFrom(configurations.testImplementation.get())
configurations[functionalTest.runtimeOnlyConfigurationName].extendsFrom(configurations.testRuntimeOnly.get())

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.12.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    add(functionalTest.implementationConfigurationName, "org.junit.jupiter:junit-jupiter:5.12.2")
    add(functionalTest.runtimeOnlyConfigurationName, "org.junit.platform:junit-platform-launcher")
    add(functionalTest.implementationConfigurationName, gradleTestKit())
}

tasks.test { useJUnitPlatform() }

val functionalTestTask = tasks.register<Test>("functionalTest") {
    description = "Runs Gradle TestKit functional tests."
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    testClassesDirs = functionalTest.output.classesDirs
    classpath = functionalTest.runtimeClasspath
    useJUnitPlatform()
    shouldRunAfter(tasks.test)
}

tasks.check { dependsOn(functionalTestTask) }

gradlePlugin {
    website.set("https://github.com/mateopedersen/betacalendars-gradle-calendar-fixtures")
    vcsUrl.set("https://github.com/mateopedersen/betacalendars-gradle-calendar-fixtures")
    plugins {
        create("calendarFixtures") {
            id = "com.betacalendars.calendar-fixtures"
            implementationClass = "com.betacalendars.gradle.calendarfixtures.CalendarFixturesPlugin"
            displayName = "Beta Calendars Calendar Fixtures"
            description = "Generate deterministic calendar JSON, CSV and Markdown fixtures and date-boundary reports with java.time. No network access at build execution."
            tags.set(listOf("calendar", "testing", "fixtures", "java-time", "build-cache", "configuration-cache"))
            compatibility {
                features {
                    configurationCache = true
                }
            }
        }
    }
}

// The Plugin Portal marker metadata is validated with the same task used for release.
