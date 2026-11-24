package io.github.andrewtran1995.liftlog.data

import org.junit.Assert.assertEquals
import org.junit.Test

class CleanNameTest {
    @Test fun trimsAndLowercases() {
        assertEquals("leg press", cleanName("  Leg Press "))
        assertEquals("arm day", cleanName("ARM DAY"))
    }

    @Test fun exerciseKeyMatchesCleanName() {
        assertEquals(cleanName("Seated Dip"), exerciseKey("seated dip "))
    }
}
