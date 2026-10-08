plugins {
    java
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

subprojects {
    apply(plugin = "java")
    apply(plugin = "com.github.spotbugs")
    apply(plugin = "pmd")

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

    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}
