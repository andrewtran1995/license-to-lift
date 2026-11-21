package io.github.andrewtran1995.liftlog

import android.app.Application
import io.github.andrewtran1995.liftlog.data.LiftLogDatabase
import io.github.andrewtran1995.liftlog.data.SessionStore

/** Holds app-wide singletons. Manual DI is plenty at this size; add Hilt or Koin if it grows. */
class LiftLogApp : Application() {
    val database: LiftLogDatabase by lazy { LiftLogDatabase.create(this) }
    val session: SessionStore by lazy { SessionStore(this) }
}
