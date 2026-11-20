# Nourish — Visual Design System

## Source of truth
- Main Cosmos collection: https://www.cosmos.so/ary70/ui
- **User-supplied screenshot:** `design/references/pastel-health-ui.png` — this is the strongest visual reference. Codex must inspect the actual local image; the Cosmos collection may be inaccessible or private.
- The reference is a **direction**, not a pixel-perfect template. Produce an original interface and do not copy brand/logo/artwork or protected assets. The screenshots depict a different kind of wellness app; reinterpret its language for food tracking.

## The look (important correction)
**Bold, playful, editorial pastel wellness UI** — NOT bland minimalist, clinical, generic green fitness tracker, empty white cards, or washed-out barely-there neutrals.

Look closely at the reference: striking **oversized bold black numerals**, cream canvas, full-bleed buttery-yellow and pink feature screens, thick black bottom navigation, big contrasting black pills, prominent pink circular + action, organic stickers (stars, hearts, petals, blobs), small dense informative chart/details, and variable-scale typographic hierarchy. It looks lively and designed, not like a standard Material template.

## Palette — initial tokens, adjust after real screenshot review
| Token | Hex | Usage |
|---|---|---|
| Canvas | `#FBF5E8` | cream/off-white base |
| Ink | `#201E21` | very dark headlines, icons, nav, buttons |
| Butter | `#F8DE70` | joyful feature page and calorie areas |
| Pink | `#F1B9D7` | protein/secondary feature hero, quick-add |
| Lilac | `#D7C7F3` | macro cards, badges |
| Blue | `#BBD3FA` | water/insight decorative cards |
| Mint | `#B7CF94` | vegetable/nutrition badges |
| Peach | `#F5C3A8` | meal/favorites accents |

Use **strong ink/pastel contrast**, not white text on pale pastel. Semantic roles, accessible status colors, and a carefully designed dark theme. Use colors intentionally; avoid rainbow noise.

## Typography
- Main numbers must feel **BIG**, bold, playful and editorial: calorie balance, protein consumed, dashboard counters. Large visual difference between hero numeric data and secondary labels.
- Distinct friendly display type + crisp body type. Choose appropriately licensed performant fonts. Consider Google Fonts from a license-compliant source and document license; fall back to system fonts if download unavailable.
- Tabular numbers for rapidly changing values. Fine-grained hierarchy: eyebrow, mini-label, display, body, helper; data remains scannable.

## Composition
- Rich UI without clutter: multiple functional layers, small labels, useful microcharts, feature chips, callouts, and highlights.
- Big feature-colored sections on some screens, cream on others. **Asymmetry** and editorial cards, rounded corners, playful stickers with irregular shapes.
- 16–24dp margins; 12–28dp radii contextually, not everything the same radius. Black nav or high-contrast bottom action dock and large central pink + button.
- Add restrained organic vectors via Compose Canvas or SVG/vector drawables; keep everything lightweight, original, and noninteractive if decorative.
- Treat edge-to-edge and status/navigation bars carefully; never obscure labels.

## Screen concepts
### Today
- Cream top section with an informal greeting and date; **hero black calorie number** with small secondary goal/remaining context.
- Daily highlights as pink/yellow/blue/green irregular sticker-like pieces, but show REAL calories/protein/carbs/fats with data labels.
- Recent meals and short weekly trend; high information density, clear hierarchy.
- Floating pink + in a dark rounded bottom navigation area; responsive press motion.

### Food logging
- Playful dark sheet launched from + with distinct colored options; fast text/voice-friendly input, camera, gallery and search. Animated opening but never delayed responsiveness.
- AI meal draft has large calories, bright food tags, editable serving amounts, clear uncertainty and Save. No fake definitive precision.

### Diary
- Cream base, strong heading, date chips, meal groups, lightweight food tiles. Small useful totals; playful category markers without confusing nutrient meanings.

### Insights
- Different pastel feature areas, with big numbers and thin, well-labeled charts. Charts must convey actual data, not only decoration.

### Settings
- Still consistent, but prioritize readability and standard Android affordances over decoration.

## Motion language
- Smooth springy bottom sheets and sheets linked to direct manipulation.
- Press: quick subtle scale/shadow/color change, optional haptic feedback; every animation may be interrupted.
- Progress: visual fill and counter interpolation after data changes, **not** after every recomposition.
- Navigation: gentle sliding/fading transitions ~180–280ms as appropriate (tune with real testing, don't hardcode unnecessary delays).
- Successful save: responsive and slightly delightful; fast-to-dismiss.
- Reduced motion: respect device settings and accessibility. No looping decorative animations, GPU-heavy blurs or particle systems. Measure frame times.

## Component contract
Build reusable Compose components: `NourishScaffold`, `NourishBottomBar`, `HeroMetric`, `PastelMetricCard`, `StickerHighlight`, `MacroProgress`, `NutritionChip`, `MealRow`, `QuickAddSheet`, `EmptyState`, `NutritionSparkline`, `PrimaryPillButton`. Names may change, but centralize design tokens and motion.

## Design acceptance and review checklist
- Screenshot of Today with demo data at a compact-phone resolution and a large phone resolution.
- Screenshots of quick-add, meal draft, Diary, Insights, dark mode, large-font mode.
- Compare against reference: **bold enough? playful enough? detailed enough? black anchors and hero color fields?**
- Verify font licenses, color contrast, tap targets >=48dp, accessible labels, no overlapping text, animation smoothness.
- UI changes require screenshot evidence in PRs. Avoid generic Material demo aesthetics.

### P5 interaction polish and Android home-screen widget

- **Page transitions.** Move smoothly between top-level destinations with a short, interruptible fade or slide. Keep the bottom navigation anchored. A quick second tap or Back gesture should take the user directly to the intended destination. Preserve each page’s scroll position and input state.
- **Press and hold.** Buttons, actionable rows, and interactive panels give immediate visual feedback on touch-down. Use a subtle scale, tint, or elevation change, maintain it while held, and spring back on release or cancellation. Dragging away cancels the action. An ordinary hold must never save twice or trigger a destructive action. Give any dedicated long-press command a visible, accessible alternative.
- **Reduced motion and access.** Honor the Android animator duration scale. With motion disabled, use an instant state change and retain clear pressed, selected, and focus indications. Verify rapid navigation, keyboard and TalkBack activation, and cancelled gestures.
- **Resizable Android home-screen widget.** Offer a launcher widget for Today. At its smallest supported size, show the date and logged calories. As the user enlarges it, reveal macro totals and optional goal progress when they fit. Use the app’s pastel fields and oversized, readable numerals. Show an honest empty day and keep labels legible in light and dark themes. A tap opens Today. Read the same local diary and refresh after saves, edits, deletions, and day changes. Verify resize in both directions and relaunch on an Android launcher.

### P3 · Vitamins, minerals and personal goals

- **Food review.** Show a Vitamins & minerals section alongside calories and macros. Support vitamins A, C, D, E, K and the B vitamins, plus minerals such as calcium, iron, magnesium, potassium, sodium and zinc. Give each nutrient a stable identity and its own unit, including mg or µg where appropriate. Show the amount for the portion actually eaten. Preserve label values, database sources and model estimates on the saved entry. An unavailable value reads “Unknown.” If only some foods report a nutrient, label the daily total as partial.
- **Choose what to see.** In Settings → Nutrients, group controls under Macros, Vitamins and Minerals. Let the user show or hide each nutrient on Today, Diary summaries and Insights. Keep the preference across restarts. Hiding iron, for example, keeps its logged amounts and goal so it can be shown again.
- **Personal targets.** Let GPT propose a daily nutrient goal with its unit, rationale, and source or assumptions. Show the current and proposed target together. The user can accept, change or skip each proposal. Apply only confirmed targets, with an effective date. Show progress for nutrients with goals and an amount for the others.

### P4 · Log from an authorized ChatGPT conversation

A user who has connected ChatGPT to Nourish attaches a food photo or barcode image in the GPT app and asks, “What’s in this, and can you log my lunch?” GPT uses the photo, the user’s description and the portion in the prompt. For a barcode, it looks up the matching product. It can read a label, retrieve nutrition from a provider, or populate a draft using its own knowledge when a lookup is incomplete. Identify the basis for each value as label, database or GPT estimate. Ask about an ambiguous product or portion when that would materially change the result.

Give GPT a read-only barcode lookup tool through MCP. In Android, give the user the same lookup through Scan or Enter barcode. Both call the same product lookup service and show the matched product, serving basis, nutrient values and source. Send only the decoded product number for an exact lookup. If the tool finds no match, ask for a label photo or an estimate before filling the draft. Manual review and save work without an AI agent.

The operator configures any provider key in the backend host’s secret store. Only the provider client reads it. Keep credentials out of the APK, source control, logs, model prompts and tool results. Limit lookup traffic and rotate a leaked key. Separately authorize the user’s Nourish connection with revocable permissions. Reading a public product grants no access to a private diary. Test that looking up a product creates no entry and that confirming a draft writes once for the connected user.

Return an editable draft in the conversation with the food, quantity, date, meal, calories, macros, and available vitamins and minerals. The user can say “half that portion,” correct individual nutrients, or approve the entry. Confirmation saves it once to the connected user’s diary and returns a receipt. Opening Nourish after sync shows the same entry and totals. Cancelling leaves the diary unchanged. Handle expired or revoked access by preserving the draft for review and offering reconnection. A suggested goal follows its own review and confirmation, separate from approving a meal.

### P5 · Interactive calendar and swipeable Today panels

- **Diary calendar.** Tap the diary date to open a month grid. Provide previous/next month controls and a Today shortcut. Mark days with logged entries, distinguish today from the selected day, and give each day an accessible label such as “November 19, two foods logged.” Selecting a date opens its meal groups and daily totals. A day without entries offers logging for that date. Month changes and selection work offline.
- **Today panels.** Swipe horizontally between Energy, Macros and the user’s visible Vitamins & minerals. Keep labeled panel selectors in view so each panel can also be reached by tapping, keyboard or TalkBack. Preserve the selected panel when returning from the diary. Use the same logged totals as the corresponding diary day. Keep vertical scrolling and the Android Back gesture usable. Respect reduced motion and show the selected panel clearly at large text sizes.
