package io.github.andrewtran1995.liftlog.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val PlateRed = Color(0xFFCF2F27)
private val PlateRedNight = Color(0xFFFF5A4A)

private val Light = lightColorScheme(primary = PlateRed, onPrimary = Color.White, background = Color(0xFFF4F3EF), surface = Color(0xFFF4F3EF))
private val Dark = darkColorScheme(primary = PlateRedNight, onPrimary = Color.Black, background = Color(0xFF121315), surface = Color(0xFF121315))

/** Plate-red brand colours by default; set [dynamicColor] to follow the wallpaper (Android 12+) instead. */
@Composable
fun LiftLogTheme(dark: Boolean = isSystemInDarkTheme(), dynamicColor: Boolean = false, content: @Composable () -> Unit) {
    val scheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(LocalContext.current) else dynamicLightColorScheme(LocalContext.current)
        dark -> Dark
        else -> Light
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
