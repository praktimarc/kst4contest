plugins {
    java
    application
    alias(libs.plugins.javafx)
}

javafx {
    version = libs.versions.javafx.get()
    modules = listOf("javafx.controls", "javafx.fxml", "javafx.web", "javafx.media")
}

dependencies {
    // Mirrors the compile-scope entries in pom.xml: 11 test classes live under
    // src/main/java and need JUnit and Mockito on the main compile classpath.
    implementation(libs.junit.jupiter.api)
    implementation(libs.mockito.core.compile)
    implementation(libs.jetbrains.annotations)
    implementation(project(":core"))
    implementation(libs.sqlite.jdbc)
    implementation(libs.jlayer)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junit.jupiter)
    // Gradle 9 no longer adds the launcher implicitly.
    testRuntimeOnly(libs.junit.platform.launcher)
}

application {
    mainClass.set("kst4contest.view.Kst4ContestApplication")
}

// Collects the application jar and its runtime dependencies into the layout the
// packaging scripts expect: a flat directory with the main jar named app.jar.
val collectRuntime by tasks.registering(Sync::class) {
    dependsOn(tasks.jar)
    from(tasks.jar) { rename { "app.jar" } }
    from(configurations.runtimeClasspath)
    into(layout.buildDirectory.dir("dist-libs"))
}

val packageImage by tasks.registering(Exec::class) {
    dependsOn(collectRuntime)
    // jpackage must come from the project toolchain, not from PATH: jdk.jsobject
    // was removed after JDK 21, and jlink then cannot resolve the module list.
    val launcher = javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(21))
    }.get()
    val jpackageTool = launcher.metadata.installationPath.file("bin/jpackage").asFile.absolutePath
    val input = layout.buildDirectory.dir("dist-libs").get().asFile
    val output = layout.buildDirectory.dir("jpackage").get().asFile
    val os = org.gradle.internal.os.OperatingSystem.current()
    val icon = when {
        os.isWindows -> "packaging/icons/kst4contest.ico"
        os.isMacOsX -> "packaging/icons/kst4contest.icns"
        else -> "packaging/icons/kst4contest.png"
    }
    doFirst { output.deleteRecursively() }
    commandLine(
        jpackageTool,
        "--icon", rootProject.file(icon).absolutePath,
        "--type", "app-image",
        "--name", "praktiKST",
        "--input", input.absolutePath,
        "--dest", output.absolutePath,
        "--main-jar", "app.jar",
        "--add-modules", providers.gradleProperty("jpackageAddModules").get(),
        "--main-class", "kst4contest.view.Main",
        "--java-options", "-Dfile.encoding=UTF-8"
    )
}
