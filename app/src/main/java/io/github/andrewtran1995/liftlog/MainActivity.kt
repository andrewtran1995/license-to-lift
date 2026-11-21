package io.github.andrewtran1995.liftlog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import io.github.andrewtran1995.liftlog.data.WorkoutDao
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

private enum class Tab(val label: String, @param:DrawableRes val icon: Int) {
    Log("Mission", R.drawable.ic_add),
    History("Dossier", R.drawable.ic_history),
    Progress("Intel", R.drawable.ic_progress),
}

@Composable
private fun LiftLogRoot(dao: WorkoutDao) {
    var tab by rememberSaveable { mutableStateOf(Tab.Log) }
    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Spy.Panel) {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = t == tab,
                        onClick = { tab = t },
                        icon = { Icon(painterResource(t.icon), contentDescription = null) },
                        label = { Text(t.label.uppercase(), style = MaterialTheme.typography.labelSmall) },
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (tab) {
                Tab.Log -> LogScreen(dao, onSaved = { tab = Tab.History })
                Tab.History -> HistoryScreen(dao)
                Tab.Progress -> Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Caption("// Intel pending")
                    Text("Progress charts are next on the roadmap", color = Spy.Muted)
                }
            }
        }
    }
}
