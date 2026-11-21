# Full-database export format

Dossier → **DATA** → **SAVE FILE** or **SHARE** writes `license-to-lift-db-YYYY-MM-DD.json`. It's a
lossless copy of the app's Room database: every row of every table, ids included, plus deleted
(tombstoned) workouts. Edit it and import it back with **MERGE** or **REPLACE ALL**.

```json
{
  "app": "license-to-lift",
  "format": "database",
  "schemaVersion": 3,
  "exportedAt": 1795290000000,
  "tables": {
    "exercise_types": [{ "id": "…", "name": "Leg curls", "nameKey": "leg curls", "createdAt": 1795… }],
    "workouts":       [{ "id": "…", "date": "2026-11-16", "name": "Leg day", "notes": "", "unit": "lb",
                         "updatedAt": 1795…, "deleted": false, "dirty": true }],
    "exercises":      [{ "id": "…", "workoutId": "…", "position": 0, "name": "Leg curls", "typeId": "…" }],
    "sets":           [{ "id": "…", "exerciseId": "…", "position": 0, "weight": 90, "reps": 10,
                         "loggedAt": 1795… }]
  }
}
```

## Fields

| Table | Field | Notes |
|---|---|---|
| `exercise_types` | `id` | Any unique string. UUIDs in practice. |
| | `name` | Display name. `nameKey` is recomputed from it on import (trimmed, lower-cased), and must be unique. |
| `workouts` | `date` | `YYYY-MM-DD`, local calendar. |
| | `unit` | `lb` or `kg`: the unit this workout's weights are in. |
| | `updatedAt` | Epoch milliseconds. Bump it when you edit a workout. |
| | `deleted` | `true` hides it (a tombstone). This is how a merge deletes a workout. |
| `exercises` | `workoutId` | Must be a workout in the same file. |
| | `typeId` | Catalog entry; `null` or unknown ids are resolved by `name` on import. |
| `sets` | `exerciseId` | Must be an exercise in the same file. |
| | `weight`, `reps` | Either may be `null`. Weight `null` is bodyweight; reps are optional. |
| | `loggedAt` | Epoch milliseconds when the set was logged, or `null`. Sets from before schema 3 have none; older files import with `null`. |

`position` orders exercises within a workout and sets within an exercise. Gaps are fine.

## Import modes

- **MERGE.** Rows in the file win. Each workout in the file replaces its copy on the phone, along
  with all its exercises and sets. Workouts not in the file are untouched. A catalog entry
  whose name already exists under another id is folded into the existing one. To delete a
  workout via merge, include it with `"deleted": true`.
- **REPLACE ALL.** Empties every table, then loads the file. The phone ends up with exactly the
  file's contents. If the workout in progress isn't in the file, the Mission tab is cleared.

Both run in one transaction: if anything fails, nothing changes.

## Rules the importer enforces

The import is refused, and nothing is written, if:

- `format` isn't `database`, or `schemaVersion` is newer than the app's;
- ids repeat within a table, or two catalog entries share a name;
- an exercise points at a workout, or a set at an exercise, that isn't in the file;
- a date isn't `YYYY-MM-DD`, or a unit isn't `lb`/`kg`.

Run `python3 tools/check_dump.py FILE` to apply the same checks before importing.

## Changing the schema

`schemaVersion` equals the Room database version (`LiftLogDatabase`, `DatabaseDump.SCHEMA_VERSION`).
When the schema changes:

1. Bump both, add a Room `Migration`, and update `DatabaseDump` (`toJson` and `parse`) and this doc.
2. Keep `parse` accepting older `schemaVersion`s by filling new fields with defaults, so old
   exports still import.
3. Teach `tools/check_dump.py` the new fields.
