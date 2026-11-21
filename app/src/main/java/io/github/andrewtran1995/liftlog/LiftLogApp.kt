package io.github.andrewtran1995.liftlog

import android.app.Application
import io.github.andrewtran1995.liftlog.data.LiftLogDatabase

/** Holds app-wide singletons. Manual DI is plenty at this size; add Hilt or Koin if it grows. */
class LiftLogApp : Application() {
    val database: LiftLogDatabase by lazy { LiftLogDatabase.create(this) }
}
