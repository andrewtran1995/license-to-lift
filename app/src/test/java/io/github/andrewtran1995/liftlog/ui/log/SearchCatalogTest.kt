package io.github.andrewtran1995.liftlog.ui.log

import io.github.andrewtran1995.liftlog.data.ExerciseTypeEntity
import io.github.andrewtran1995.liftlog.data.exerciseKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SearchCatalogTest {
    private fun t(name: String) = ExerciseTypeEntity(name, name, exerciseKey(name), 0L)
    private val catalog = listOf(t("Chest press"), t("Shoulder press"), t("Leg press"), t("Pulldown"), t("Press-up"))

    @Test fun emptyQueryHasNoMatches() {
        val r = searchCatalog("  ", catalog)
        assertNull(r.exact)
        assertEquals(emptyList<ExerciseTypeEntity>(), r.matches)
    }

    @Test fun startsWithComesBeforeContains() {
        assertEquals(listOf("Press-up", "Chest press", "Leg press", "Shoulder press"), searchCatalog("press", catalog).matches.map { it.name })
    }

    @Test fun exactMatchIgnoresCaseAndSpaces() {
        assertEquals("Pulldown", searchCatalog(" pULLdown ", catalog).exact?.name)
        assertNull(searchCatalog("pull", catalog).exact)
    }
}
