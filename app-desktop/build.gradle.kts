plugins {
    java
    alias(libs.plugins.javafx)
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeMultiplatform)
}

javafx {
    version = libs.versions.javafx.get()
    modules = listOf("javafx.controls", "javafx.fxml", "javafx.web", "javafx.media")
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation("org.openjfx:javafx-swing:${libs.versions.javafx.get()}")
    // Mirrors the compile-scope entries in pom.xml: 11 test classes live under
    // src/main/java and need JUnit and Mockito on the main compile classpath.
    implementation(libs.junit.jupiter.api)
    implementation(libs.mockito.core.compile)
    implementation(libs.jetbrains.annotations)
    implementation(project(":core"))
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(libs.sqlite.jdbc)
    implementation(libs.jlayer)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junit.jupiter)
    // Gradle 9 no longer adds the launcher implicitly.
    testRuntimeOnly(libs.junit.platform.launcher)
}

compose.desktop {
    application {
        // Main and not Kst4ContestApplication: an Application subclass cannot be
        // started from the classpath, which is where JavaFX lives since Etappe 1.
        mainClass = "kst4contest.view.Main"

        nativeDistributions {
            /*
             * Only the formats the building machine can actually produce. Compose rejects a
             * foreign one while CONFIGURING the project, not while packaging it, so a list
             * naming all five made every Gradle task fail on macOS and Windows — including
             * `run` and `test`, which package nothing. The build was Linux-only without
             * anyone having decided that.
             */
            val host = org.gradle.internal.os.OperatingSystem.current()
            targetFormats(
                *when {
                    host.isMacOsX -> arrayOf(
                        org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg,
                    )
                    host.isWindows -> arrayOf(
                        org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
                    )
                    else -> arrayOf(
                        org.jetbrains.compose.desktop.application.dsl.TargetFormat.AppImage,
                        org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb,
                        org.jetbrains.compose.desktop.application.dsl.TargetFormat.Rpm,
                    )
                }
            )
            /*
             * jlink must come from JDK 21, not from whatever the Gradle JVM is.
             * jdk.jsobject was removed after 21, and this machine has 26 on PATH;
             * Etappe 1 hit the same wall with jpackage.
             */
            javaHome = javaToolchains.launcherFor {
                languageVersion.set(JavaLanguageVersion.of(21))
            }.get().metadata.installationPath.asFile.absolutePath

            /*
             * KST4Contest and not praktiKST: this one name drives the app-image
             * directory, the launcher, the .app bundle, the bundled icon file
             * and -- lowercased -- the deb/rpm package name. Every packaging
             * consumer already uses KST4Contest there (AppRun, the Arch and
             * Flatpak wrappers, the AUR PKGBUILDs, build-signed-dmg.sh), and the
             * shipped deb/rpm must keep the package identity "kst4contest" so an
             * installed copy still upgrades.
             */
            packageName = "KST4Contest"
            packageVersion = providers.gradleProperty("composePackageVersion").get()

            /*
             * Kept from the former module-info.java, minus the javafx.* modules.
             * jdk.jsobject carries netscape.javascript for the map bridge, and
             * jdk.unsupported carries sun.misc.Unsafe, without which the JavaFX
             * Marlin renderer fails to start. Neither is inferred: JavaFX comes
             * from the classpath, so nothing declares them.
             */
            modules(
                "java.desktop", "java.net.http", "java.sql",
                "jdk.crypto.ec", "jdk.jsobject", "jdk.net",
                "jdk.xml.dom", "jdk.unsupported",
            )

            linux { iconFile.set(rootProject.file("packaging/icons/kst4contest.png")) }
            windows { iconFile.set(rootProject.file("packaging/icons/kst4contest.ico")) }
            macOS {
                iconFile.set(rootProject.file("packaging/icons/kst4contest.icns"))
                // Must stay de.x08.KST4Contest: build-signed-dmg.sh signs against it.
                bundleID = "de.x08.KST4Contest"
            }
        }
    }
}
