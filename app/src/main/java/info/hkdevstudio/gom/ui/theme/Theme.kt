package info.hkdevstudio.gom.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import info.hkdevstudio.gom.R

// ---- 1b 디자인 토큰: Colors ----
val Cream = Color(0xFFFBF7F0)
val CreamMap = Color(0xFFF4EFE6)
val Sand = Color(0xFFEDE4D8)
val SandDeep = Color(0xFFDDD3C6)
val Ink = Color(0xFF2A2420)
val Ink2 = Color(0xFF3A322D)
val Mute = Color(0xFF7A6F66)
val Mute2 = Color(0xFFA59A90)
val Paprika = Color(0xFFD9532B)
val PaprikaLight = Color(0xFFF0894F)
val PaprikaDeep = Color(0xFFB9401F)
val PaprikaTint = Color(0xFFFBE6DD)
val PaprikaTintBorder = Color(0xFFF5A97A)
val HeartOff = Color(0xFFC9BFB4)

/** 룰렛 섹터 팔레트: 후보 i → RoulettePalette[i % 8] */
val RoulettePalette = listOf(
    Color(0xFFD9532B), Color(0xFFF0894F), Color(0xFFB9401F), Color(0xFFF5A97A),
    Color(0xFFC84A26), Color(0xFFE8703F), Color(0xFFA63A1A), Color(0xFFF29A66),
)

// ---- Typography ----
/** Display: Jua (단일 굵기). 본문은 기기 기본 산세리프(국내 기기 Noto Sans CJK 계열). */
val Jua = FontFamily(Font(R.font.jua, FontWeight.Normal))

object GomType {
    val displayL = TextStyle(fontFamily = Jua, fontSize = 44.sp, lineHeight = 50.sp, color = Cream)
    val displayM = TextStyle(fontFamily = Jua, fontSize = 38.sp, lineHeight = 44.sp, color = Ink)
    val displayTitle = TextStyle(fontFamily = Jua, fontSize = 34.sp, lineHeight = 40.sp, color = Ink)
    val displayS = TextStyle(fontFamily = Jua, fontSize = 30.sp, lineHeight = 35.sp, color = Ink)
    val sheetTitle = TextStyle(fontFamily = Jua, fontSize = 28.sp, lineHeight = 32.sp, color = Ink)
    val numeralL = TextStyle(fontFamily = Jua, fontSize = 22.sp, lineHeight = 26.sp, color = Ink)
    val numeral = TextStyle(fontFamily = Jua, fontSize = 20.sp, lineHeight = 24.sp, color = Ink)
    val titleM = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Bold, color = Ink)
    val titleS = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold, color = Ink)
    val body = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal, color = Ink)
    val bodyS = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Normal, color = Ink)
    val meta = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Normal, color = Mute)
    val badge = TextStyle(fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium, color = PaprikaDeep)
    val button = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold, color = Cream)
}

private val GomTypography = Typography(
    displayLarge = GomType.displayL,
    displayMedium = GomType.displayM,
    displaySmall = GomType.displayS,
    headlineMedium = GomType.displayTitle,
    headlineSmall = GomType.sheetTitle,
    titleMedium = GomType.titleM,
    titleSmall = GomType.titleS,
    bodyMedium = GomType.body,
    bodySmall = GomType.bodyS,
    labelMedium = GomType.meta,
    labelSmall = GomType.badge,
    labelLarge = GomType.button,
)

private val GomShapes = Shapes(
    extraSmall = RoundedCornerShape(9.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

private val GomColorScheme = lightColorScheme(
    primary = Paprika,
    onPrimary = Color.White,
    primaryContainer = PaprikaTint,
    onPrimaryContainer = PaprikaDeep,
    secondary = Ink,
    onSecondary = Cream,
    surface = Cream,
    onSurface = Ink,
    surfaceVariant = Color.White,
    onSurfaceVariant = Mute,
    background = Cream,
    onBackground = Ink,
    outline = Sand,
    outlineVariant = SandDeep,
    scrim = Color(0xFF2A2420),
)

@Composable
fun GomTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = GomColorScheme,
        typography = GomTypography,
        shapes = GomShapes,
        content = content,
    )
}
