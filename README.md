# License to Lift

A native Android app for logging gym workouts (exercises, sets, weight and optional reps by date) and tracking progress over time. It's the native successor to the Lift Log web app, styled as a Q-Lab bench at night: a blueprint-grid backdrop, a refracting glass nav bar (Haze), translucent instrument panels, Material 3 Expressive motion, and JetBrains Mono throughout.

## Stack

| Layer | Choice | Why |
|---|---|---|
| Language | Kotlin | Google's default for Android |
| UI | Jetpack Compose + Material 3 Expressive, Haze glass | Declarative UI with spring motion and a refracting glass nav bar |
| Storage | Room (SQLite) | Offline by default, with reactive `Flow` queries |
| Build | AGP 9 (built-in Kotlin), Gradle version catalog, KSP | Current build setup, with no kapt |
| CI | GitHub Actions | Builds a debug APK on every push to `main` |

Min SDK 29 (Android 10), target and compile SDK 37.

## Run it

1. Open the folder in a current Android Studio and let Gradle sync.
2. Plug in your phone with USB debugging on, then press **Run**.

Or skip Studio: open the latest **Build** run under the repo's **Actions** tab, download the `lift-log-debug` artifact and install the APK on your phone.

## Data model

`workouts` → `exercises` → `sets`, with UUID string ids. Weight and reps on a set are both nullable, so a set can be just a weight and an exercise can have no sets (e.g. "Knee lifts: done"). Each workout stores the unit (`lb`/`kg`) it was entered in.

`updatedAt`, `dirty` and the `deleted` tombstone already exist so that a sync backend can be added later without a schema rewrite.

## Status

- [x] Room schema and DAO
- [x] Live logging: one exercise at a time, prefilled sets, reps stepper and quick picks, autosave
- [x] History list with delete
- [x] Edit a past workout (resume it from the Dossier)
- [x] Exercise catalog (discrete, addable) with "last time" and one-tap reuse
- [x] Intel: per-exercise progress (heaviest, volume, reps) with chart
- [ ] Settings: units (DataStore)
- [x] Import a backup from the web app (paste or file; idempotent)
- [ ] Sync (see below)

## Sync

The web version syncs through Claude's artifact storage, which only works inside claude.ai, so a native app can't use it. Options, simplest first:

1. **Local only + Android backup.** `allowBackup` is on, so the Room database is included in Google's device backup.
2. **JSON export/import** to a file in Drive or Downloads.
3. **A real backend** (e.g. Supabase or Firebase) using the `dirty` and `deleted` columns. Push dirty rows, pull newer rows, newest `updatedAt` wins. That's the same scheme the web app uses.

## Credits

JetBrains Mono is bundled under the SIL Open Font License 1.1 (`licenses/OFL-JetBrainsMono.txt`).
