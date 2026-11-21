package io.github.andrewtran1995.liftlog.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Remembers which workout is in progress, so a mission survives the app being closed or killed.
 * The workout itself lives in Room and is saved set by set; this only holds its id.
 */
class SessionStore(context: Context) {
    private val prefs = context.getSharedPreferences("session", Context.MODE_PRIVATE)
    private val _activeId = MutableStateFlow(prefs.getString(KEY, null))
    val activeId: StateFlow<String?> = _activeId.asStateFlow()

    fun set(id: String?) {
        prefs.edit().apply { if (id == null) remove(KEY) else putString(KEY, id) }.apply()
        _activeId.value = id
    }

    private companion object { const val KEY = "activeWorkoutId" }
}
