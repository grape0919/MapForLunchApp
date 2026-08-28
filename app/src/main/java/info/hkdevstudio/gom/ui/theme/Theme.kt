package info.hkdevstudio.gom.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Mint = Color(0xFF0E9B82)
val MintDark = Color(0xFF0B7A67)
val MintSoft = Color(0xFFE2F3EF)
val Ink = Color(0xFF1F2B28)

private val GomColorScheme = lightColorScheme(
    primary = Mint,
    onPrimary = Color.White,
    primaryContainer = MintSoft,
    onPrimaryContainer = MintDark,
    secondary = MintDark,
    surface = Color.White,
    onSurface = Ink,
    background = Color(0xFFF7FAF9),
    onBackground = Ink,
)

@Composable
fun GomTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = GomColorScheme,
        content = content,
    )
}
