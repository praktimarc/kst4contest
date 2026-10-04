plugins {
    java
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeMultiplatform)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    // Mirrors the compile-scope entries in pom.xml: a few test classes still live under
    // src/main/java and need JUnit and Mockito on the main compile classpath.
    implementation(libs.junit.jupiter.api)
    implementation(libs.mockito.core.compile)
    implementation(libs.jetbrains.annotations)
    implementation(project(":core"))
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(libs.sqlite.jdbc)
    implementation(libs.jlayer)
    /*
     * The Compose UI test harness, used through runComposeUiTest { } from ordinary Jupiter
     * tests. Deliberately NOT ui-test-junit4: that one brings the vintage engine, and two
     * engines on one classpath is how half a suite silently stops being run. Checked after
     * adding this: the only engine resolved is junit-jupiter-engine. JUnit 4 classes are on
     * the test classpath, but they were before this too — something else pulls them
     * transitively — and with no vintage engine nothing executes them.
     *
     * The accessor is marked experimental by the Compose Gradle plugin, so the opt-in sits
     * here — one place, for one dependency. What it buys is the only mechanism that can
     * answer "did the value reach the screen": the state-level tests and the two settings
     * coverage nets check that a setter is called, which is a different question.
     */
    @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
    testImplementation(compose.uiTest)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junit.jupiter)
    // Gradle 9 no longer adds the launcher implicitly.
    testRuntimeOnly(libs.junit.platform.launcher)
}

compose.desktop {
    application {
        // Main forwards to Kst4ContestApplication.main. It was a workaround for
        // launching a JavaFX Application subclass from the classpath; it stays because
        // the packaged launchers and the AUR/Flatpak wrappers name this class.
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
             * Kept from the former module-info.java. jdk.jsobject went with the map
             * bridge: it carried netscape.javascript, and nothing imports that any
             * more. jdk.unsupported stays — its justification used to be the JavaFX
             * Marlin renderer, but sun.misc.Unsafe is reached by other dependencies
             * too, and dropping it is a separate decision from removing JavaFX.
             */
            modules(
                "java.desktop", "java.net.http", "java.sql",
                "jdk.crypto.ec", "jdk.net",
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
