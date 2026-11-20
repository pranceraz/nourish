# Nourish

Nourish is an offline Android food diary. Tap the pink **+**, enter a food label per serving or per 100 g, then enter how much you ate. Save to see the meal and daily totals. Open **Diary → Actions** to edit, duplicate, or remove an entry. Tap the date to switch days. Saved foods are available under **+**, with favorites first.

Room stores entries and food labels in the app’s private `databases/diary.db`. Each entry retains the label used when it was logged. Preferences DataStore stores personal goals and the demo switch under `files/datastore/`. The first Room schema is checked in under `app/schemas`. P0 had no diary database. Its saved demo preference migrates on upgrade. Fresh installations show your own diary. Clearing storage or uninstalling removes local data. There is no cloud backup.

## Faster food entry in v0.3.0

Tap **+** and search your saved foods by name or product barcode. **Scan package barcode** opens the phone’s camera. Fill the frame with a clear UPC or EAN, then confirm the photo. You can also type the digits and tap **Use barcode**. For a new product, enter its label and save once. Future scans find that saved label offline.

**Take photo** and **Choose photo** attach a meal picture to a draft. Review the photo and enter the nutrition before saving. Reopen **Diary → Actions → Edit** to view, replace, or remove it. Nourish keeps a resized JPEG in its private database and strips original metadata. The camera gets temporary access to a capture file, and the picker shares only the photo you select. Neither action needs broad library access.

The built-in nutrition provider searches this device’s saved foods. Photos are visual references. Automatic estimation is planned for P3.

## Build locally

Open the project root in Android Studio or use JDK 21 with Android SDK Platform **37.2**, Build Tools **37.0.0**, and Platform Tools.

```sh
sdkmanager 'platforms;android-37.2' 'build-tools;37.0.0' 'platform-tools'
./gradlew :app:lintDebug :app:testDebugUnitTest :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Use an Android 8.0 (API 26) or newer device. Run the Compose instrumented test with a connected device or emulator using `./gradlew :app:connectedDebugAndroidTest`. Android Studio previews are in `NourishPreviews.kt`, including dark theme, compact and large phones, large text, and the major tabs.

## Install a GitHub build

1. Open the PR’s **Checks** tab or the repository’s **Actions** tab and open a successful **Android CI** run.
2. Download and extract the **nourish-debug-…** artifact. Install `app-debug.apk` on your Android phone, allowing installation from the source you use to open it if prompted, or install it with `adb install -r app-debug.apk`.
3. Open **Settings → About** to confirm the version name, version code, and first 12 characters of the build’s Git commit. A build from a source archive can show `local` in place of a commit.

CI artifacts are kept for 14 days. See [Testing builds](docs/TESTING.md) for device checks.

## Toolchain and assets

Dependencies are pinned in the [version catalog](gradle/libs.versions.toml). The build uses [AGP 9.5](https://developer.android.com/build/releases/agp-9-5-0-release-notes), [Gradle 9.9.2](https://services.gradle.org/versions/current), [Kotlin 2.4.21](https://kotlinlang.org/docs/releases.html), and the [Compose BOM](https://developer.android.com/develop/ui/compose/bom/bom-mapping).

The display font is a static ExtraBold instance of [Bricolage Grotesque](https://github.com/google/fonts/tree/main/ofl/bricolagegrotesque) under the [SIL Open Font License](docs/licenses/BricolageGrotesque-OFL.txt). Body text uses Android’s system sans serif. App artwork, icons, and stickers are original vector paths. See the [design system](design/DESIGN.md).
