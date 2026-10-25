package com.example.ui.theme

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

private val DarkColorScheme = darkColorScheme(
  primary = AlpineTealPrimaryDark,
  secondary = AlpineTealSecondaryDark,
  tertiary = AlpineTealTertiaryDark,
  background = AlpineDarkBackground,
  surface = AlpineDarkSurface,
  surfaceVariant = Color(0xFF263345),
  onPrimary = Color(0xFF003833),
  onSecondary = Color(0xFF003733),
  onBackground = Color(0xFFF1F5F9),
  onSurface = Color(0xFFF8FAFC),
  onSurfaceVariant = Color(0xFF94A3B8),
  primaryContainer = Color(0xFF134E4A),
  onPrimaryContainer = Color(0xFFCCFBF1),
  outline = Color(0xFF475569),
  outlineVariant = Color(0xFF334155)
)

private val LightColorScheme = lightColorScheme(
  primary = AlpineTealPrimary,
  secondary = AlpineTealSecondary,
  tertiary = AlpineTealTertiary,
  background = AlpineLightBackground,
  surface = AlpineLightSurface,
  surfaceVariant = Color(0xFFF1F5F9),
  onPrimary = Color.White,
  onSecondary = Color.White,
  onBackground = Color(0xFF0F172A),
  onSurface = Color(0xFF1E293B),
  onSurfaceVariant = Color(0xFF64748B),
  primaryContainer = Color(0xFFCCFBF1),
  onPrimaryContainer = Color(0xFF0F766E),
  outline = Color(0xFFCBD5E1),
  outlineVariant = Color(0xFFE2E8F0)
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = when {
    dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
      val context = LocalContext.current
      if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
    darkTheme -> DarkColorScheme
    else -> LightColorScheme
  }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
