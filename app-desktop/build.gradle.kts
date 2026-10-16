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
    // JUnit and Mockito are test-only. Until #62 a few test classes sat under src/main/java
    // and dragged both -- plus byte-buddy and objenesis -- into lib/app of every app image.
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

/*
 * The translation files are turned into Kotlin before anything compiles, by the
 * :i18n-generator module. Two consequences worth naming:
 *
 * - A broken translation fails `./gradlew classes`, which is what the PR workflow already
 *   runs -- so a contributor's mistake is reported in their pull request without a new
 *   workflow existing for it.
 * - The generator is reached through its own configuration, not through `implementation`, so
 *   it stays out of the application's runtime classpath and out of the jar.
 */
val i18nGenerator: Configuration by configurations.creating

dependencies {
    i18nGenerator(project(":i18n-generator"))
}

val generatedI18nDirectory = layout.buildDirectory.dir("generated/i18n")

val generateStrings by tasks.registering(JavaExec::class) {
    description = "Generates Strings.kt from app-desktop/src/main/i18n"
    group = "build"

    classpath = i18nGenerator
    mainClass.set("kst4contest.i18n.GeneratorMain")

    val input = layout.projectDirectory.dir("src/main/i18n")
    // A local copy: the provider below must not capture the script-level val, which
    // drags the build script object into the configuration cache and fails to serialize.
    val output = generatedI18nDirectory
    inputs.dir(input)
    outputs.dir(output)

    argumentProviders.add(CommandLineArgumentProvider {
        listOf(input.asFile.absolutePath, output.get().asFile.absolutePath)
    })
}

kotlin.sourceSets["main"].kotlin.srcDir(generatedI18nDirectory)

tasks.named("compileKotlin") { dependsOn(generateStrings) }

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

            linux {
                iconFile.set(rootProject.file("packaging/icons/kst4contest.png"))
                /*
                 * Package release number behind the version (1.50.0-<release>).
                 * The version itself is composePackageVersion for every target
                 * format, so a beta DEB/RPM would otherwise carry exactly the
                 * version of the following stable one -- and dnf, rpm -U and
                 * dpkg refuse to replace a package by one of equal version, so
                 * a beta user could not move to the stable (#113). The tagged
                 * release workflow therefore passes -PlinuxAppRelease following
                 * the Fedora pre-release convention:
                 *
                 *   beta-v1.50.0-beta-001  ->  1.50.0-0.beta.001
                 *   beta-v1.50.0-rc02      ->  1.50.0-0.rc02
                 *   v1.50.0                ->  1.50.0-1
                 *
                 * Everything starting with "0." sorts below "1" for both rpm
                 * and dpkg. Nightlies and local builds do not set the property
                 * and keep release 1.
                 */
                appRelease = providers.gradleProperty("linuxAppRelease").orElse("1").get()
            }
            windows {
                iconFile.set(rootProject.file("packaging/icons/kst4contest.ico"))
                /*
                 * The MSI installer is built for tagged releases only; nightlies ship
                 * the portable ZIP alone, because an MSI always carries the plain
                 * composePackageVersion and two nightly MSIs could not be told apart.
                 * It installs per user, so it needs neither administrator rights nor
                 * a UAC prompt, and it adds a Start menu group and a desktop shortcut.
                 *
                 * upgradeUuid must NEVER change: Windows Installer uses it to
                 * recognise an installed KST4Contest and replace it in place when a
                 * newer MSI runs. A different value would install the new release
                 * side by side with the old one instead of upgrading it.
                 */
                upgradeUuid = "3375071a-25ba-468e-9ae5-8296bc535462"
                menu = true
                menuGroup = "KST4Contest"
                shortcut = true
                perUserInstall = true
                dirChooser = true
            }
            macOS {
                iconFile.set(rootProject.file("packaging/icons/kst4contest.icns"))
                // Must stay de.x08.KST4Contest: build-signed-dmg.sh signs against it.
                bundleID = "de.x08.KST4Contest"
            }
        }
    }
}
