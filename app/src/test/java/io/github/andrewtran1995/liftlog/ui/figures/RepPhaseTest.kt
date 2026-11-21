package io.github.andrewtran1995.liftlog.ui.figures

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Guards the loop contract every figure relies on: a rep ends exactly where it starts. */
class RepPhaseTest {
    @Test fun startAndEndAreTheSamePose() {
        assertEquals(0f, repPhase(0f), 0f)
        assertEquals(0f, repPhase(1f), 0f)
    }

    @Test fun peakIsMidRep() {
        assertEquals(1f, repPhase(0.5f), 1e-4f)
    }

    @Test fun easesInAndOutAtTheSeam() {
        // Tiny steps either side of the wrap barely move the pose: no visible jerk.
        assertTrue(repPhase(0.01f) < 0.01f)
        assertTrue(repPhase(0.99f) < 0.01f)
    }

    @Test fun symmetricThereAndBack() {
        for (i in 0..50) {
            val t = i / 100f
            assertEquals(repPhase(t), repPhase(1f - t), 1e-5f)
        }
    }

    @Test fun classifiesCatalogNames() {
        assertEquals(FigureKind.LegCurl, figureFor("Seated leg curls"))
        assertEquals(FigureKind.Adductor, figureFor("Adductor machine"))
        assertEquals(FigureKind.Abduction, figureFor("Hip abduction"))
        assertEquals(null, figureFor("Deadlift"))
    }
}
