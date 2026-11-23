# AGENTS.md

Instructions for coding agents working in this repo (Claude Code reads it through `CLAUDE.md`).

## Project

License to Lift: a native Android gym log. Kotlin, Jetpack Compose with Material 3 Expressive,
Room, Haze glass. See `README.md` for the stack and data model.

## Building and verifying

- There may be no Android SDK where you run. Verify by pushing to `main`: GitHub Actions runs
  `testDebugUnitTest` then `assembleRelease`, and publishes a release `build-N` with
  `license-to-lift.apk`. A change isn't done until that run is green.
- Read compile errors from the job log (`gh api repos/<owner>/<repo>/actions/jobs/<id>/logs`, grep `^e: `).
- Every build is signed with `signing/license-to-lift.jks` so it installs over the last one.
  Never change or regenerate that key: it would force the user to uninstall and lose data.
- Room schema changes need a version bump and a `Migration` in `LiftLogDatabase.kt` that keeps
  existing data. Never use destructive migration.

## Text style

Space Grotesk (`Sans`) for words, JetBrains Mono (`Mono`) for numbers. Use `.numbers()` on a
text style for anything mostly digits (weights, reps, set numbers, stats) so columns line up.
Text is lowercase (titles, captions, labels, tabs, exercise names); buttons that do something
stay UPPERCASE. Helper sentences keep normal sentence case.

## Working with exported databases

The user may send a full-database export (`license-to-lift-db-*.json`, format in
`docs/database-format.md`) to edit, migrate or inspect, then import it back on the phone.

- **Edit with a script, not by hand.** Load the JSON in Python, change the rows, and write it
  back with the same structure. Keep every existing id. New rows get new UUIDs. Bump
  `updatedAt` on any workout you change.
- **Keep references whole.** Every exercise needs its workout, and every set its exercise, in the
  same file. Catalog names must stay unique (trimmed, case-insensitive).
- **To delete a workout, set `"deleted": true`** rather than dropping its rows, so MERGE removes
  it on the phone too.
- **Check before handing it back:** `python3 tools/check_dump.py FILE` must print "OK to import".
- **Tell the user which import mode to use.** MERGE for additions and edits; REPLACE ALL only when
  the file is the complete intended database (e.g. after a migration or a large cleanup).
- **New exercises in the catalog** may need a figure: see the next section.

## Exercise figures

Low-poly animated figures live in `app/src/main/java/.../ui/figures/ExerciseFigure.kt`. To add
one for a new exercise:

1. **Add a `FigureKind`** and a keyword match in `figureFor()`. Match loosely ("curl" + "leg"),
   so variants of a name ("Seated leg curls") find the same figure. Add a case to `RepPhaseTest`.
2. **Draw it in the 160 × 140 design space.** Origin is the top left, y points down, and
   rotation is clockwise-positive about a pivot (same as SVG). Keep every point inside the
   space at both phase 0 and phase 1; work out the joint endpoints at both phases before
   writing code.
3. **Use the shared parts.** Draw with `head()` (hexagon with a dark facet on its right half),
   `poly()` for faceted torsos (base `Body`, right/far half `Facet`), `limb()` for limbs (round
   caps; thigh 8–9, upper arm 7, forearm 6 units wide), `box()` for machine parts. The near
   side is `Body`, the far side `Facet`. The machine is `Machine` / `MachineDark`. Only the
   contact point (pad, roller, handle, dumbbell) uses `Pad`.
4. **Pick a view.** Side view faces right. Front view is symmetric: draw one side, then repeat
   it inside `mirrored { }` with the `Facet` colour.
5. **Animate with joints, not keyframes.** Nest `rotate(lerp(rest, peak, p), pivot = joint)`
   from the root joint outwards (hip → knee, shoulder → elbow). A child joint that must keep its
   absolute angle cancels its parent's rotation (for example, the shin turns +95° while the
   thigh turns −95°).

### The loop rule

Figures must loop perfectly: a rep ends in exactly the pose it started from, with no jump.

- **Pose depends only on the phase.** Every angle and offset is a function of `p` alone, where
  `p = repPhase(raw)`. Nothing reads time, frame count or random values.
- **Phase 0 is the start pose and 1 is the peak.** `repPhase` goes 0 → 1 → 0 over one rep,
  eased at both ends, so raw 0 and raw 1 are the same pose at zero speed and reps join
  seamlessly. Don't change that function; `RepPhaseTest` enforces it.
- **Never snap to a pose other than raw 0 or 1.** To restart mid-rep, finish the rep first
  (the existing `LaunchedEffect` does this).
- **Playback:** two reps when `playKey` changes or on tap, ending at the start pose. Static
  icons draw the peak (raw 0.5). No infinite loops: a looping figure redraws every frame and
  makes the glass nav bar re-capture the screen each frame.

### Preview before porting

It's quicker to get the poses right as an SVG first. Use the same 160 × 140 viewBox and colours,
and put each joint in a `<g>` with
`<animateTransform type="rotate" values="REST cx cy;PEAK cx cy;REST cx cy" keyTimes="0;0.5;1" calcMode="spline" keySplines="0.45 0 0.55 1;0.45 0 0.55 1" dur="2.4s" repeatCount="indefinite"/>`,
nested in the same order as the Compose code. Values that start and end on the same angle loop
perfectly here too. Then port each `<g>` to a `rotate(...)` block with the same numbers.
