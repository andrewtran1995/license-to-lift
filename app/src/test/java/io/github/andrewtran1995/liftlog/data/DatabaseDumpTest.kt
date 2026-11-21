package io.github.andrewtran1995.liftlog.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class DatabaseDumpTest {
    private val sample = DatabaseDump(
        schemaVersion = DatabaseDump.SCHEMA_VERSION,
        exportedAt = 1L,
        types = listOf(ExerciseTypeEntity("t1", "Leg curls", "leg curls", 10L)),
        workouts = listOf(WorkoutEntity("w1", "2026-11-16", "Leg day", "felt good", "lb", 20L, deleted = false, dirty = true)),
        exercises = listOf(
            ExerciseEntity("e1", "w1", 0, "Leg curls", "t1"),
            ExerciseEntity("e2", "w1", 1, "Knee lifts", null),
        ),
        sets = listOf(SetEntity("s1", "e1", 0, 90.0, 10, loggedAt = 1_795_290_000_000L), SetEntity("s2", "e1", 1, 90.0, null)),
    )

    @Test fun roundTripKeepsEveryRow() {
        val back = DatabaseDump.parse(DatabaseDump.toJson(sample))
        assertEquals(sample.copy(exportedAt = back.exportedAt), back)
    }

    @Test fun detectsFormat() {
        assertTrue(DatabaseDump.looksLikeDump(DatabaseDump.toJson(sample)))
        assertFalse(DatabaseDump.looksLikeDump("""{"app":"liftlog","v":1,"sessions":[]}"""))
        assertFalse(DatabaseDump.looksLikeDump("not json"))
    }

    @Test fun rejectsNewerSchema() {
        val json = DatabaseDump.toJson(sample).replace("\"schemaVersion\": ${DatabaseDump.SCHEMA_VERSION}", "\"schemaVersion\": 99")
        assertThrows(IllegalArgumentException::class.java) { DatabaseDump.parse(json) }
    }

    @Test fun rejectsOrphans() {
        val orphanSet = sample.copy(sets = sample.sets + SetEntity("s9", "missing", 0, 1.0, 1))
        assertThrows(IllegalArgumentException::class.java) { DatabaseDump.parse(DatabaseDump.toJson(orphanSet)) }
        val orphanExercise = sample.copy(exercises = sample.exercises + ExerciseEntity("e9", "nope", 0, "X", null))
        assertThrows(IllegalArgumentException::class.java) { DatabaseDump.parse(DatabaseDump.toJson(orphanExercise)) }
    }

    @Test fun rejectsDuplicateCatalogNames() {
        val dupe = sample.copy(types = sample.types + ExerciseTypeEntity("t2", "leg curls ", "leg curls", 11L))
        assertThrows(IllegalArgumentException::class.java) { DatabaseDump.parse(DatabaseDump.toJson(dupe)) }
    }

    @Test fun recomputesNameKeyFromName() {
        val edited = DatabaseDump.toJson(sample).replace("\"nameKey\": \"leg curls\"", "\"nameKey\": \"stale\"")
        assertEquals("leg curls", DatabaseDump.parse(edited).types.single().nameKey)
    }

    @Test fun olderSchemaSetsHaveNoTime() {
        val v2 = """{"app":"license-to-lift","format":"database","schemaVersion":2,"exportedAt":1,"tables":{
            "workouts":[{"id":"w1","date":"2026-11-16","unit":"lb"}],
            "exercises":[{"id":"e1","workoutId":"w1","position":0,"name":"Leg curls","typeId":null}],
            "sets":[{"id":"s1","exerciseId":"e1","position":0,"weight":90,"reps":10}]}}"""
        assertEquals(null, DatabaseDump.parse(v2).sets.single().loggedAt)
    }
}
