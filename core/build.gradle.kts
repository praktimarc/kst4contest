plugins {
    java
}

// No user-interface technology here: the domain layer observes through
// kst4contest.observe and hands UI work over via UiDispatcher.

dependencies {
    // JUnit and Mockito are test-only. Until #62 a handful of test classes sat under
    // src/main/java and dragged both onto the runtime classpath of the shipped app.
    implementation(libs.jetbrains.annotations)
    implementation(libs.sqlite.jdbc)
    implementation(libs.jlayer)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junit.jupiter)
    // Gradle 9 no longer adds the launcher implicitly.
    testRuntimeOnly(libs.junit.platform.launcher)
}
