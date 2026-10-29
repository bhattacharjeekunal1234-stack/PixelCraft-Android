// Root build file: declares plugin versions once for all modules.
//
// Version pairing (checked against the official compatibility tables):
// - Gradle 9.5.1 (see gradle/wrapper/gradle-wrapper.properties) runs on the
//   JDK bundled with current Android Studio releases, and is the newest
//   line AGP 8.x supports (Gradle 9.6 removed an internal API AGP 8 uses).
// - AGP 8.13 supports compileSdk 36.
// - Kotlin 2.2.x is the Kotlin Gradle Plugin line compatible with Gradle 9.
plugins {
    id("com.android.application") version "8.13.0" apply false
    id("org.jetbrains.kotlin.android") version "2.2.20" apply false
}
