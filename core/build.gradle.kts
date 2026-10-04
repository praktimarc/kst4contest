plugins {
    java
}

// No user-interface technology here: the domain layer observes through
// kst4contest.observe and hands UI work over via UiDispatcher.

dependencies {
    // Test classes live under src/main/java in this project, so JUnit and Mockito
    // are needed on the main compile classpath. This mirrored pom.xml before Gradle.
    implementation(libs.junit.jupiter.api)
    implementation(libs.mockito.core.compile)
    implementation(libs.jetbrains.annotations)
    implementation(libs.sqlite.jdbc)
    implementation(libs.jlayer)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junit.jupiter)
    // Gradle 9 no longer adds the launcher implicitly.
    testRuntimeOnly(libs.junit.platform.launcher)
}
