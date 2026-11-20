plugins {
    alias(libs.plugins.ksp)
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

val gitRevision = providers.exec {
    workingDir = rootDir
    commandLine("git", "rev-parse", "--verify", "HEAD")
    isIgnoreExitValue = true
}.standardOutput.asText.map(String::trim)

val buildId = providers.environmentVariable("NOURISH_BUILD_ID")
    .orElse(gitRevision)
    .map { revision -> revision.takeIf { it.matches(Regex("[a-fA-F0-9]{7,40}")) } ?: "local" }
    .orElse("local")

android {
    namespace = "com.nourish.app"
    compileSdk {
        version = release(37) {
            minorApiLevel = 2
        }
    }
    buildToolsVersion = "37.0.0"

    defaultConfig {
        applicationId = "com.nourish.app"
        minSdk = 26
        targetSdk = 37
        versionCode = 5
        versionName = "0.5.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "BUILD_ID", "\"${buildId.get()}\"")
    }

    sourceSets["androidTest"].assets.srcDir("schemas")

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

ksp { arg("room.schemaLocation", file("schemas").absolutePath) }

dependencies {
    implementation(libs.room.runtime)
    ksp(libs.room.compiler)
    implementation(libs.preferences)
    implementation(libs.barcode.core)
    implementation(libs.exif)
    implementation(platform(libs.compose.bom))
    implementation(libs.activity.compose)
    implementation(libs.compose.animation)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui)
    implementation(libs.compose.preview)

    debugImplementation(libs.compose.tooling)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.runner)
}
