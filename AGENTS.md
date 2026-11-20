# Instructions for Codex working in Nourish

Read **`SPEC.md`**, **`design/DESIGN.md`**, **`docs/TESTING.md`**, and the screenshot in `design/references/` before implementation. User's latest design preference (bold, information-rich playful pastel) overrides earlier minimalist ideas.

## Execution
- This is a native **Android Kotlin + Jetpack Compose** project. Do not replace it with Flutter, React Native, a web wrapper, or a mocked app.
- Start with a short plan and proceed to implement a **working vertical slice**. Don't stop at diagrams or generate only stubs.
- For unfamiliar APIs/dependencies, verify current stable versions and compatibility in official documentation when network access exists; do not invent versions or claim a network check happened if blocked.
- Prefer current, well-maintained AndroidX components, local Room persistence, Flow and ViewModels, and readable idiomatic Kotlin. Use a version catalog and Gradle Wrapper. Avoid unnecessary abstraction or module explosion.
- Test after each substantial change. Report actual command outputs, failure reasons, and anything unverified.
- Never embed server API secrets in the APK, commit tokens, or expose personal food data in logs/screenshots/artifacts.
- User-visible functionality requires honest data, not fake food entries or placeholders portrayed as real. Explicit demo data is acceptable for Compose previews only.
- Don't silently replace existing changes; inspect git status and code before editing. Keep changes on a feature branch / PR; don't rewrite `main` carelessly.
- When adding CI, it must build the **actual** Android project. No empty workflow that claims success while skipping compilation.

## First task
Follow `prompts/START_HERE.md`. Deliver P0 first: real Gradle app + documented design system + visually polished Today screen + navigation and quick-add shell, Compose previews, tests, and GitHub Actions debug APK. P1 follows as a separate change after P0 builds.

## Completion report
Always include: implemented features, files changed, screenshots/previews, exact tests executed/results, how to get APK from GitHub Actions, limitations, and recommended next incremental PR.
