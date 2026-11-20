# Nourish — Testing builds and comparing versions

## Mental model
- Codex writes code and opens a GitHub pull request (PR).
- GitHub Actions starts temporary runners, compiles and tests code, and publishes a debug APK **artifact** for each successful PR build.
- GitHub **does not store your nutrition diary**. Room stores local diary records in private app storage on the phone. Later, authenticated cloud sync can store a backup/cross-device copy.

## Requirements for Codex CI setup (after scaffolding)
Create `.github/workflows/android-ci.yml` with triggers on PR and push to `main` and a manual trigger. Use pinned actions, a compatible JDK (typically 17 unless verified otherwise), Android SDK/Gradle Wrapper, Gradle caching; run Android lint, unit tests, `:app:assembleDebug`; fail on errors; upload APK and useful reports as artifacts (with a short retention period); never upload signing credentials or private data. If emulator UI tests are feasible in CI, add later rather than blocking the first build.

## Test a build without Android Studio
1. Push to GitHub/open a PR; open the repo **Actions** tab.
2. Open the latest successful Android CI run.
3. Under **Artifacts**, download and extract the debug APK.
4. Transfer it to your Android phone and install. Android might require you to permit installing apps from that source.
5. Run actual user flows and report screenshots/bugs to Codex. Debug APK is for personal testing, not Play Store release.

## Test with Android Studio (optional)
- Clone repo, install Android SDK, open `android/` or project root as applicable.
- Run on emulator/connected phone. `./gradlew :app:assembleDebug` and `./gradlew testDebugUnitTest` (or correct flavor-specific tasks), plus lint and device UI tests.
- Use Android profiler and Macrobenchmark for meaningful speed measurements.

## Compare versions safely
- Keep stable `main` and create branches such as `design/playful-dashboard` and `feature/offline-diary`.
- GitHub Actions produces APK per branch/PR. Record commit SHA and `versionName`/`versionCode` in Settings > About.
- Eventually add a distinct preview application ID (e.g. a `.preview` suffix), so stable and experimental apps install side by side. Their data stores must be separate. Do NOT silently uninstall production builds with real unsynced data.
- Use golden/screenshot tests with deterministic fixtures to compare the same screens.

## Manual acceptance checklist
- Startup and nav; quick-add responsiveness; text truncation; light/dark theme; large font and TalkBack.
- Offline add/edit/delete meal with real persistence; force-close and relaunch; duplicate-save taps; empty/error states.
- Network slow/failure; denied camera permission; large photo; interrupted analysis.
- Room migration tests when schema changes; export/backup tested before destructive operations.

## Versions
- `v0.1.0` P0 visual shell + CI debug APK.
- `v0.2.0` P1 fully offline diary.
- `v0.3.0` P2 local food search, UPC/EAN lookup and meal photo attachments. AI drafts follow in P3.
- `v0.4.0` account, cloud sync, MCP integration.
Versions are **suggested milestones, not claims of implemented features**.

## P2 device checks

1. Upgrade a P1 install signed with the same key. Check that a favorite and an existing diary entry remain.
2. In airplane mode, scan a UPC/EAN or type its digits. Save its label, scan again, change the portion, and check the total. Search by part of the food name.
3. Take or pick a photo. Cancel once and verify the draft remains. Save a photo, restart, then use Diary → Actions → Edit to view or remove it. A duplicate retains its own photo after the original is deleted.
4. With no camera handler, an unreadable barcode, or an inaccessible image, verify that manual logging still works. The emulator test supplies synthetic external camera/picker results. Use a physical phone to evaluate camera focus and lighting.
