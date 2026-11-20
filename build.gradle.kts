buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        // Keep AGP's built-in Kotlin and the Compose compiler on the same release.
        classpath(libs.kotlin.gradle.plugin)
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.compose.compiler) apply false
}
