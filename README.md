# Nourish

Nourish is a native Android nutrition journal in development. The P0 build shows sample calories, macros, meals, and a seven-day trend on Today. Tap the pink **+** to add a sample food and see the totals change. You can hide the sample data in **Settings → Demo data**. Sample additions last for the current app session.

## Build locally

Open the project root in Android Studio or use JDK 21 with Android SDK Platform **37.2**, Build Tools **37.0.0**, and Platform Tools.

```sh
sdkmanager 'platforms;android-37.2' 'build-tools;37.0.0' 'platform-tools'
./gradlew :app:lintDebug :app:testDebugUnitTest :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Use an Android 8.0 (API 26) or newer device. Run the Compose instrumented test with a connected device or emulator using `./gradlew :app:connectedDebugAndroidTest`. Android Studio previews are in `NourishPreviews.kt`, including dark theme, compact and large phones, large text, and both quick-add views.

## Install a GitHub build

1. Open the PR’s **Checks** tab or the repository’s **Actions** tab and open a successful **Android CI** run.
2. Download and extract the **nourish-debug-…** artifact. Install `app-debug.apk` on your Android phone, allowing installation from the source you use to open it if prompted, or install it with `adb install -r app-debug.apk`.
3. Open **Settings → About** to confirm the version name, version code, and first 12 characters of the build’s Git commit. A build from a source archive can show `local` in place of a commit.

CI artifacts are kept for 14 days. See [Testing builds](docs/TESTING.md) for device checks.

## Toolchain and assets

Dependencies are pinned in the [version catalog](gradle/libs.versions.toml). The build uses [AGP 9.5](https://developer.android.com/build/releases/agp-9-5-0-release-notes), [Gradle 9.9.2](https://services.gradle.org/versions/current), [Kotlin 2.4.21](https://kotlinlang.org/docs/releases.html), and the [Compose BOM](https://developer.android.com/develop/ui/compose/bom/bom-mapping).

The display font is a static ExtraBold instance of [Bricolage Grotesque](https://github.com/google/fonts/tree/main/ofl/bricolagegrotesque) under the [SIL Open Font License](docs/licenses/BricolageGrotesque-OFL.txt). Body text uses Android’s system sans serif. App artwork, icons, and stickers are original vector paths. See the [design system](design/DESIGN.md).
