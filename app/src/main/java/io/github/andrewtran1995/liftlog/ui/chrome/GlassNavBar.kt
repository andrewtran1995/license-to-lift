package io.github.andrewtran1995.liftlog.ui.chrome

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.glass.GlassStyle
import dev.chrisbanes.haze.glass.hazeGlass
import io.github.andrewtran1995.liftlog.ui.theme.Spy

data class NavItem(val label: String, @param:DrawableRes val icon: Int)

private val BarShape = RoundedCornerShape(30.dp)
private val PillShape = RoundedCornerShape(24.dp)

/**
 * Glass instrument bar: the backdrop refracts through it like a lens. Reads the captured
 * hazeSource (Sources) rather than the window backdrop: Haze's own benchmarks found Sources
 * cheaper on the CPU for Glass. The selection pill slides between tabs on the theme's expressive spring.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GlassNavBar(
    items: List<NavItem>,
    selected: Int,
    onSelect: (Int) -> Unit,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
) {
    // The built-in Regular glass follows the phone's light/dark setting; this app is always dark,
    // so pin the dark response and add a faint cyan lens tint.
    val glass = remember {
        GlassStyle.regular.then {
            shape(BarShape)
            tint(Color(0x2605080D))
            specularIntensity(0.38f)
            edgeShadow(Color.Black.copy(alpha = 0.32f))
            ambientResponse(0.08f)
            contrast(-0.31f)
            whitePoint(-0.23f)
            chromaMultiplier(1.25f)
        }
    }
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(68.dp)
            .hazeGlass(input = HazeInput.Sources(hazeState), style = glass)
            .border(1.dp, Color.White.copy(alpha = 0.10f), BarShape),
    ) {
        val inset = 6.dp
        val slot = (maxWidth - inset * 2) / items.size
        val pillX by animateDpAsState(inset + slot * selected, MaterialTheme.motionScheme.defaultSpatialSpec(), label = "pill")
        Box(
            Modifier
                .offset(x = pillX)
                .width(slot)
                .fillMaxHeight()
                .padding(vertical = inset)
                .background(Spy.Signal.copy(alpha = 0.14f), PillShape)
                .border(1.dp, Spy.Signal.copy(alpha = 0.45f), PillShape),
        )
        Row(Modifier.fillMaxSize().padding(horizontal = inset), horizontalArrangement = Arrangement.SpaceEvenly) {
            items.forEachIndexed { i, item ->
                val on = i == selected
                val tint by animateColorAsState(if (on) Spy.Signal else Spy.Muted, MaterialTheme.motionScheme.defaultEffectsSpec(), label = "tint")
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .selectable(
                            selected = on,
                            onClick = { onSelect(i) },
                            role = Role.Tab,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(painterResource(item.icon), contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
                    Text(item.label.uppercase(), style = MaterialTheme.typography.labelSmall, color = tint, maxLines = 1)
                }
            }
        }
    }
}
