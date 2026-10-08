rootProject.name = "kst4contest"

include(":core")
include(":app-desktop")

/*
 * Build tooling only: this module generates the Kotlin for the translation files and is never
 * shipped -- app-desktop reaches it through its own configuration, not through implementation.
 * It is a module rather than buildSrc because buildSrc's tests do not run on
 * `./gradlew build` -- measured -- and a generator whose tests never run is the exact failure
 * it exists to prevent.
 */
include(":i18n-generator")
