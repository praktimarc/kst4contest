plugins {
    java
    // Applied here as well as in the subprojects: the root needs the JacocoReport task
    // type and the tool classpath for the aggregated report below.
    jacoco
    alias(libs.plugins.spotbugs) apply false
}

allprojects {
    group = providers.gradleProperty("group").get()
    version = providers.gradleProperty("version").get()

    repositories {
        mavenCentral()
        // Compose Multiplatform pulls its androidx.lifecycle and androidx.annotation
        // artifacts from Google's repository; they are not on Maven Central.
        google()
    }
}

val pmdVersion = libs.versions.pmd.get()
val spotbugsVersion = libs.versions.spotbugs.get()
val jacocoVersion = libs.versions.jacoco.get()

subprojects {
    apply(plugin = "java")
    apply(plugin = "com.github.spotbugs")
    apply(plugin = "pmd")
    apply(plugin = "jacoco")

    // Both tools report only; the Maven build had the failing goals commented out.
    extensions.configure<PmdExtension> {
        toolVersion = pmdVersion
        ruleSetFiles = files(rootProject.file("pmd-ruleset.xml"))
        ruleSets = emptyList()
        isIgnoreFailures = true
    }

    extensions.configure<com.github.spotbugs.snom.SpotBugsExtension> {
        toolVersion.set(spotbugsVersion)
        ignoreFailures.set(true)
    }

    // Without a configured report SpotBugs prints its findings to stdout only and leaves
    // no file behind; with ignoreFailures they then scroll past unread. AGENTS.md asks for
    // the findings visible in a build to be reported, which needs something to read them
    // from: XML for CI to count, HTML for people. Default location, which the plugin names
    // after the source set: <module>/build/reports/spotbugs/main.{xml,html} (#110).
    tasks.withType<com.github.spotbugs.snom.SpotBugsTask>().configureEach {
        reports.create("xml") { required.set(true) }
        reports.create("html") { required.set(true) }
    }

    /*
     * Coverage is a report, not a gate: there is no jacocoTestCoverageVerification and no
     * minimum percentage. A threshold set at today's number would trip on the next unrelated
     * change; one set at a target would fail every build until tests are written for the
     * number's sake. The figure is published so it can be read when deciding what to test
     * next (docs/superpowers/specs/2026-10-11-test-coverage-design.md, #62).
     */
    extensions.configure<JacocoPluginExtension> {
        toolVersion = jacocoVersion
    }

    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }

    // XML for CI to read, HTML for people. Gradle does not wire the report after the test
    // task on its own; without this dependsOn the report task would read stale or missing
    // execution data. Kotlin classes count too: the Kotlin plugin adds its output to the
    // main source set, which is what the default report reads.
    tasks.withType<JacocoReport>().configureEach {
        dependsOn(tasks.withType<Test>())
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
    }
}

/*
 * One report over the two shipped modules. i18n-generator stays out: it is a build tool,
 * and its coverage says nothing about the application. Sources and classes are resolved
 * lazily because the root script is evaluated before the subprojects have their source sets.
 */
val coveredProjects = listOf(project(":core"), project(":app-desktop"))

val jacocoAggregatedReport by tasks.registering(JacocoReport::class) {
    description = "Merges the JaCoCo coverage of core and app-desktop into one report"
    group = "verification"

    dependsOn(coveredProjects.map { it.tasks.named("test") })

    executionData.setFrom(coveredProjects.map { it.layout.buildDirectory.file("jacoco/test.exec") })
    sourceDirectories.setFrom(provider {
        coveredProjects.flatMap { it.extensions.getByType<SourceSetContainer>()["main"].allSource.srcDirs }
    })
    classDirectories.setFrom(provider {
        coveredProjects.map { it.extensions.getByType<SourceSetContainer>()["main"].output }
    })

    reports {
        xml.required.set(true)
        xml.outputLocation.set(layout.buildDirectory.file("reports/jacoco/aggregated/jacoco.xml"))
        html.required.set(true)
        html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/aggregated/html"))
    }
}
