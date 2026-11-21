# Performance audit

Date: 2026-11-21. Scope: the whole app at commit "Live logging" (Mission, Dossier, Intel, glass chrome).
Method: code review against Compose and Haze performance guidance. No on-device trace was taken, so
the findings are ranked by expected impact, not measured. Run the checks at the end on a phone to
confirm.

## Findings

| # | Impact | Finding | Fix | Status |
|---|---|---|---|---|
| 1 | High | **CI shipped a debug APK.** Debug builds are debuggable and not R8-optimised, so Compose runs much slower than in a release build; this alone commonly shows up as janky animation. | Build `assembleRelease` (R8 minify and resource shrinking) and publish that. | Fixed |
| 2 | High | **No baseline profiles on a sideloaded APK.** Play installs get cloud profiles; a sideloaded APK runs Compose's hot paths interpreted/JIT until the runtime warms up, so the first minutes after each install stutter most. | Add `androidx.profileinstaller` so the profiles Compose and other libraries ship are compiled on install. A custom app baseline profile (Macrobenchmark module) would help further. | Fixed (library profiles); custom profile open |
| 3 | Medium | **The glass bar re-captures its source every frame anything inside it animates**: tab crossfades, scrolling lists, the reps stepper. That's inherent to live glass. | Read `HazeInput.Sources` instead of `Backdrop` (Haze's benchmarks show Sources 23–46% cheaper on CPU for Glass); drop the extra chromatic-aberration override; shorten the tab crossfade. If it still drops frames: `performanceMode = HazePerformanceMode.Performance`, or plain blur instead of glass. | Fixed (first three) |
| 4 | Medium | **Backdrop grid rebuilt every draw.** ~100 `drawLine` calls plus two gradient allocations each time Haze re-records the source. | `drawWithCache`: build two paths and both gradients once per size, then replay. | Fixed |
| 5 | Low | **Intel recomputed its series on every recomposition** (each chip tap, each scroll frame that recomposes). | `remember(workouts, selected)`. | Fixed |
| 6 | Low | **Dossier list had no item keys**, so inserts and deletes rebind every card. | Key by workout id. | Fixed |
| 7 | Low (grows with data) | **Every logged set re-runs three full queries**: the active workout, the whole history with all exercises and sets (for "last time" and "repeat"), and the catalog-by-recency join. Fine for months of data, noticeable after years. | Replace the history flow on the Mission screen with targeted queries (latest entry per exercise type; last 3 workouts). | Open |
| 8 | Info | Font: JetBrains Mono is four static TTFs, loaded once. No issue. | — | — |
| 9 | Info | ViewModel survives tab switches; screens recompose only on their own state. No accidental per-frame recomposition found in the nav bar (pill offset and tint are animated values read in layout and draw). | — | — |

## How to check on your phone

1. **Profile GPU rendering.** Developer options → *Profile HWUI rendering* → *On screen as bars*.
   Scroll the Dossier and switch tabs. Bars above the green line are missed frames.
2. **Compare with the glass off.** If the bars only spike near the nav bar, finding 3 is the cost.
   Switching that bar to `HazePerformanceMode.Performance` is a one-line change.
3. **Proper numbers.** A Macrobenchmark module (`FrameTimingMetric` while scrolling and switching
   tabs) on a device gives P50/P90 frame times; it is also what generates a custom baseline profile.
