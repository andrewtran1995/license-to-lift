package io.github.andrewtran1995.liftlog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import io.github.andrewtran1995.liftlog.data.WorkoutDao
import io.github.andrewtran1995.liftlog.ui.chrome.BlueprintBackdrop
import io.github.andrewtran1995.liftlog.ui.chrome.GlassNavBar
import io.github.andrewtran1995.liftlog.ui.chrome.NavItem
import io.github.andrewtran1995.liftlog.ui.history.HistoryScreen
import io.github.andrewtran1995.liftlog.ui.log.LogScreen
import io.github.andrewtran1995.liftlog.ui.theme.Caption
import io.github.andrewtran1995.liftlog.ui.theme.LiftLogTheme
import io.github.andrewtran1995.liftlog.ui.theme.Spy

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val dao = (application as LiftLogApp).database.workoutDao()
        setContent { LiftLogTheme { LiftLogRoot(dao) } }
    }
}

private val Tabs = listOf(
    NavItem("Mission", R.drawable.ic_add),
    NavItem("Dossier", R.drawable.ic_history),
    NavItem("Intel", R.drawable.ic_progress),
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LiftLogRoot(dao: WorkoutDao) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val hazeState = rememberHazeState()
    val status = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val nav = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    // Screens scroll under the status bar and the floating glass bar, so they pad for both.
    val content = PaddingValues(start = 16.dp, end = 16.dp, top = status + 16.dp, bottom = nav + 110.dp)

    Box(Modifier.fillMaxSize()) {
        // Everything inside this box is what the glass bar refracts.
        Box(Modifier.fillMaxSize().hazeSource(hazeState)) {
            BlueprintBackdrop(Modifier.fillMaxSize())
            AnimatedContent(
                targetState = tab,
                transitionSpec = {
                    fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()) togetherWith
                        fadeOut(MaterialTheme.motionScheme.fastEffectsSpec())
                },
                label = "tab",
            ) { t ->
                when (t) {
                    0 -> LogScreen(dao, content, onSaved = { tab = 1 })
                    1 -> HistoryScreen(dao, content)
                    else -> Box(Modifier.fillMaxSize().padding(content), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Caption("// Intel pending")
                            Text("Progress charts are next on the roadmap", color = Spy.Muted)
                        }
                    }
                }
            }
        }
        GlassNavBar(
            items = Tabs,
            selected = tab,
            onSelect = { tab = it },
            hazeState = hazeState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 20.dp, vertical = 14.dp),
        )
    }
}
