package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val BentoColorScheme = lightColorScheme(
  primary = BentoDeepPurple,
  secondary = NeonRed,
  tertiary = NeonYellow,
  background = BentoBackground,
  surface = BentoCardPink,
  onBackground = BentoTextPrimary,
  onSurface = BentoTextPrimary,
  primaryContainer = BentoNavActivePill,
  surfaceVariant = BentoCardGray,
  outline = BentoCardGrayBorder,
  onPrimary = BentoWhite,
  onSecondary = BentoWhite,
  onTertiary = BentoTextBlue
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = false, // Force a consistent gorgeous Bento light theme
  dynamicColor: Boolean = false, // Disable dynamic content overlay to enforce pixel-perfect bento colors
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = BentoColorScheme,
    typography = Typography,
    content = content
  )
}
