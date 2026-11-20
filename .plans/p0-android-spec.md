# Nourish P0 implementation decisions

This records the decisions from the P0 design discussion. [SPEC.md](../SPEC.md) defines the product phases.

- **Today.** The supplied wellness screenshot guides the scale, pastel color fields, stickers, and dark bottom dock. The [Cosmos UI collection](https://www.cosmos.so/ary70/ui) also informs the compact chart labels and alternation of quiet and saturated surfaces. Use original artwork and labeled sample calories, macro goals, meals, and a seven-day energy chart.
- **Demo data.** Show a clear label on Today and put the switch in Settings. Remember the switch on the device. Keep sample additions in session state.
- **Quick Add.** The central pink + opens a bottom sheet. In P0, users can select a sample food and add it to demo totals. Future input methods appear as informational text. The offline diary is the next implementation phase.
- **Future inputs.** Explore a camera and photo-picker entry point, written or dictated meal descriptions, and a review step for estimated foods. For Claude or ChatGPT integration, consider a verified Android App Link to open a specific reviewable draft after authenticated handoff. Keep any food details out of the link itself.
- **Verification.** Build a real APK with the Gradle Wrapper. Run lint, unit tests, and the Compose device test where tooling permits. Use compact, large, dark, and large-text previews for review.
