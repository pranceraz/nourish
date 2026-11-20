# Nourish — Product and Engineering Specification

**Status:** implementation-ready specification · November 2026

## Mission
Build a beautiful, **native Android** nutrition journal that makes food logging effortless. Users can photograph food, describe meals conversationally, scan a barcode, or enter meals manually. Calories and macros must be traceable, editable, and persisted. A secure backend and MCP integration eventually allow ChatGPT to read and update the same diary.

**Non-goals for first release:** social feed, advertisements, paid subscriptions, universal health advice, an unrestricted agent, proprietary calorie prediction model, and an iOS/web client.

## Product principles
1. **Complete flows before many screens.** Never ship a dead button or a fake save operation.
2. **Offline-first.** Manual logging, diary, targets, and dashboard work without connectivity.
3. **Photo estimation is uncertain.** Vision detects candidate foods/portions; nutrition data comes from a source or is explicitly marked estimated. User reviews and confirms before save.
4. **Frictionless.** Logging starts in one tap from Today; recent/favorite foods are quick to reuse.
5. **Fast and small.** Native UI, modest dependencies, no blocking animation or network call on main thread.
6. **Privacy.** No advertising SDKs, minimal permissions, no embedded API secrets, local data export/delete.
7. **Native Android.** Kotlin, Jetpack Compose, Material 3 foundations, accessible, responsive, light/dark themes.

## Stack (verify stable versions at implementation time)
- Android: Kotlin, Jetpack Compose, Gradle version catalog, AndroidX Lifecycle/ViewModel/StateFlow, Coroutines, Room, DataStore, CameraX, Android Photo Picker, WorkManager when needed, dependency injection only where it reduces complexity.
- Architecture: feature-oriented packages, unidirectional state, repositories and clear domain models. Do **not** over-modularize an MVP.
- Tests: JUnit, Android instrumentation + Compose UI tests; screenshot previews for major states; later baseline profiles and Macrobenchmark on representative device.
- Backend (later phase): Python, FastAPI, Pydantic, database migrations, PostgreSQL for hosted data, authenticated users, OpenAI Responses API image analysis, USDA FoodData Central nutrition data, optional additional licensed providers, official MCP SDK.
- Android must never contain a secret OpenAI or nutrition-service server credential. Keep local app usable without backend.

## Core UX

### Today / Dashboard
- Big, bold daily calorie figure and goal/remaining context; macro goals for protein/carbs/fat, not just one metric.
- Useful, information-rich but friendly highlights; quick-add; meals today; status of recent nutrition logs.
- Never display fabricated streaks, progress, intake, or insights. Seeded demo content is clearly marked as demo and can be disabled.

### Quick Add
- Prominent add action; options: **Describe food**, **Take photo**, **Choose photo**, **Search food**, **Scan barcode**.
- Text/photo produces a *draft*, editable food names and quantities, calories/macros, provenance, clarifying questions when necessary, and a Save confirmation.
- Show analysis states and failures; cancel/retry; don't double-save on retries.

### Diary / Food entry
- Date switcher; breakfast/lunch/dinner/snacks; add/edit/delete/duplicate entries; custom food; favorites/recent meals; portion units and raw/cooked distinction.
- Every meal and food item should have a stable ID. Totals calculated deterministically from food ingredients, quantities, and source values.
- Food entries survive process death, rotation, and reboot; always work offline.

### Insights
- Actual historical daily/weekly/monthly calories and macros, goal adherence, sensible visualizations. Don't suggest medically prescriptive targets.

### Settings
- Calorie and macro goals; units; theme; export/import; delete data; version/build info; privacy; optional backups/sync status.

## Domain models (suggested)
- `Food` (id, name, source, sourceId, per100g nutrients or custom serving nutrients, flags for estimates)
- `Serving` (id, foodId, amount, unit, gramEquivalent)
- `Meal` (id, timestampUtc, localDate/timezone, type, notes, optional imageRef)
- `MealItem` (id, mealId, foodId, quantity, unit, nutrientSnapshot, certainty/source)
- `NutritionGoal` (date effective, calorie/protein/carb/fat targets)
- `WeightEntry` (optional)
- `SyncOperation` (later phase; idempotency key, timestamp, action, status)

Define units precisely; distinguish grams from servings, raw vs cooked; prevent invalid or negative quantities; never use floating-point equality for database change detection. Store historical nutrient snapshots so edited external nutrition data doesn't silently rewrite past meals. Provide migrations and tests.

## Phases, in strict vertical slices

### P0 — Android foundation + design proof
- Scaffold a buildable native Kotlin/Compose project with correct Gradle Wrapper and pinned compatible dependencies.
- Implement design tokens from `design/DESIGN.md`, Today screen with an explicit demo-data mode, quick add sheet, Diary shell, Insights shell, Settings shell, previews.
- Include accessibility contrast and dynamic text testing; meaningful pressed/empty/loading motion.
- Create GitHub Actions CI **after** Gradle project exists: lint, unit tests, assemble debug APK, upload APK as workflow artifact. Do not check in binaries.

### P1 — Offline diary (usable app)
- Room schema/migrations, DataStore preferences and goals, manual food creation, add/edit/delete/duplicate meal items, local rollups, date navigation, favorites/recent, graceful empty states.
- Real persistence and deterministic macro calculations; no AI or server required.
- Validate persistence over restart, offline behavior, duplicate taps and deletions.

### P2 — Input accelerators
- Food search using locally cached entries and a provider adapter; barcode scanner when nutrition provider is configured; camera/gallery intake, permissions/error handling. Core manual flow remains functional offline.

### P3 — AI food drafts

Lead with everyday ingredients and mixed dishes. Search an offline USDA survey-food catalog without a key. Review estimated ingredients and portions with one combined total, then save as separate items or as one complete dish. Keep the optional personal USDA key in the authenticated user’s server-side vault for a read-only GPT lookup through MCP. Continue packaged-product provider expansion later.
- Build minimal FastAPI backend with typed API schemas, a fake provider for local tests, OpenAI vision for food identification and textual parsing, USDA lookup, normalized nutrient calculation, draft review/edit/confirm in Android.
- No privileged API secrets in APK. Clearly label estimates, ingredients/quantity assumptions, uncertain image results. Keep API logging sanitized.
- Extend nutritional snapshots, portion calculations and rollups to vitamins and minerals, with explicit units, provenance, and support for unknown or partial values. Persist per-nutrient display choices. Add reviewable GPT-suggested targets with user confirmation and effective dates.

### P4 — Accounts, cloud sync, MCP
- Choose hosted PostgreSQL (e.g. Supabase) and secure per-user authentication. Design one source-of-truth, offline operations queue, conflict handling, deletion tombstones, idempotent retries.
- MCP remote tools: `search_food`, `log_meal`, `get_daily_summary`, `get_meals`, `edit_meal`, `delete_meal`, `get_nutrition_trends`. Enforce user ownership and confirmation for destructive writes. ChatGPT integration depends on account compatibility; no claim that it works until verified.
- The phone's Room database is private; ChatGPT connects through authenticated server-side services, not directly to the phone.
- In an authorized ChatGPT conversation, accept a meal photo or barcode and a portion prompt. GPT can populate a draft from a label, provider lookup or clearly identified model estimates, including micronutrients. Support conversational corrections, one confirmed write, and a receipt that resolves to the same entry in Nourish. Allow separately confirmed goal proposals through the connection.

### P5 — Polish and release readiness
- Animated nutrient progress, refined sheets/gestures, screenshots, accessibility audit, release configurations, realistic startup/scroll benchmarks, Baseline Profiles, export/backup/delete, notification opt-in.
- Deliver interruptible page transitions, press-and-hold feedback on interactive controls, and a resizable Android home-screen widget showing Today’s local totals. Follow the P5 interaction and widget requirements in `design/DESIGN.md`.
- Add an interactive offline Diary month calendar and horizontally swipeable Today panels with labeled alternatives to gestures. Use each user’s nutrient visibility preferences. See `design/DESIGN.md` for navigation and accessibility behavior.

## Acceptance criteria by phase
- P0: Gradle build succeeds; home preview matches the design direction; navigation works; APK available from CI; no placeholder interactions misrepresented as features.
- P1: user logs a meal offline, closes/reopens app, edits it, sees correct totals, deletes it; tests cover same scenarios.
- P3: hiding and restoring a nutrient preserves its history and goal. Partial source coverage is visible. Proposed targets take effect after confirmation. Photo/text creates a **reviewable** draft, source values labeled; confirmation saves once; network failures don't destroy local data.
- P4: a cloud-authorized MCP write updates the same diary after Android sync, with isolation between test users.
- P5: selecting a logged or empty day opens the correct diary date. Swiping and tapping panel selectors show identical totals and both work with reduced motion.

## Delivery requirements for Codex
Every PR should summarize completed scope, screenshots for UI changes, exact commands run/test results, a downloadable debug APK through CI after P0, known issues, and next step. Prefer small reviewable PRs. `main` stays buildable. Never claim unrun tests passed or inaccessible design content was inspected.
