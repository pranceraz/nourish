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
