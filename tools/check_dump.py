#!/usr/bin/env python3
"""Check a License to Lift full-database export before importing it into the app.

Usage: python3 tools/check_dump.py FILE [--schema N]

Applies the same rules as DatabaseDump.parse (docs/database-format.md) and prints a summary.
Exits 1 if the app would refuse the file.
"""
import json
import re
import sys
from collections import Counter

SCHEMA_VERSION = 3  # keep equal to DatabaseDump.SCHEMA_VERSION


def check(doc, schema=SCHEMA_VERSION):
    errors = []
    if doc.get("format") != "database":
        return ["format is not \"database\""]
    v = doc.get("schemaVersion")
    if not isinstance(v, int) or v < 1:
        errors.append("missing or bad schemaVersion")
    elif v > schema:
        errors.append(f"schemaVersion {v} is newer than the app ({schema})")
    t = doc.get("tables") or {}
    types, workouts = t.get("exercise_types", []), t.get("workouts", [])
    exercises, sets = t.get("exercises", []), t.get("sets", [])

    for name, rows, req in [
        ("exercise_types", types, ["id", "name"]),
        ("workouts", workouts, ["id", "date"]),
        ("exercises", exercises, ["id", "workoutId", "name"]),
        ("sets", sets, ["id", "exerciseId"]),
    ]:
        for i, r in enumerate(rows):
            for k in req:
                if not str(r.get(k, "")).strip():
                    errors.append(f"{name}[{i}] missing {k}")
        dup = [k for k, c in Counter(r.get("id") for r in rows).items() if c > 1]
        if dup:
            errors.append(f"{name} duplicate ids: {dup[:3]}")

    keys = Counter(str(r.get("name", "")).strip().lower() for r in types)
    errors += [f"exercise_types: two entries named {k!r}" for k, c in keys.items() if c > 1]
    for w in workouts:
        if not re.fullmatch(r"\d{4}-\d{2}-\d{2}", str(w.get("date", ""))):
            errors.append(f"workout {w.get('id')} bad date {w.get('date')!r}")
        if w.get("unit", "lb") not in ("lb", "kg"):
            errors.append(f"workout {w.get('id')} unit {w.get('unit')!r}")
    wids = {w.get("id") for w in workouts}
    eids = {e.get("id") for e in exercises}
    errors += [f"exercise {e.get('id')} -> missing workout {e.get('workoutId')}" for e in exercises if e.get("workoutId") not in wids]
    for s in sets:
        t = s.get("loggedAt")
        if t is not None and (isinstance(t, bool) or not isinstance(t, int) or t < 0):
            errors.append(f"set {s.get('id')} loggedAt {t!r} is not epoch milliseconds or null")
    errors += [f"set {s.get('id')} -> missing exercise {s.get('exerciseId')}" for s in sets if s.get("exerciseId") not in eids]
    return errors


def main():
    args = sys.argv[1:]
    if not args:
        print(__doc__)
        sys.exit(2)
    schema = SCHEMA_VERSION
    if "--schema" in args:
        schema = int(args[args.index("--schema") + 1])
    with open(args[0], encoding="utf-8") as f:
        doc = json.load(f)
    errors = check(doc, schema)
    t = doc.get("tables", {})
    live = [w for w in t.get("workouts", []) if not w.get("deleted")]
    print(f"{len(live)} missions ({len(t.get('workouts', [])) - len(live)} deleted), "
          f"{len(t.get('exercises', []))} exercise entries, {len(t.get('sets', []))} sets, "
          f"{len(t.get('exercise_types', []))} catalog exercises")
    if errors:
        print("Would be refused:")
        for e in errors:
            print("  -", e)
        sys.exit(1)
    print("OK to import")


if __name__ == "__main__":
    main()
