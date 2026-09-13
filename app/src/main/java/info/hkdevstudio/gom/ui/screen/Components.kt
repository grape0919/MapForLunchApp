package info.hkdevstudio.gom.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import info.hkdevstudio.gom.BuildConfig
import info.hkdevstudio.gom.domain.Place
import info.hkdevstudio.gom.ui.theme.Cream
import info.hkdevstudio.gom.ui.theme.GomType
import info.hkdevstudio.gom.ui.theme.Ink
import info.hkdevstudio.gom.ui.theme.Paprika
import info.hkdevstudio.gom.ui.theme.Sand
import info.hkdevstudio.gom.ui.theme.SandDeep
import info.hkdevstudio.gom.util.GeoUtils

// ---- 텍스트 헬퍼 ----

/** "일식 · 140m · 도보 2분" */
fun Place.metaLine(): String = listOfNotNull(
    category.ifBlank { null },
    distanceM?.let { "${it}m" },
    distanceM?.let { "도보 ${GeoUtils.walkMinutes(it)}분" },
).joinToString(" · ")

// ---- 기본 요소 ----

/** 로고 타일: Ink 배경 + Jua "밥" */
@Composable
fun LogoTile(size: Dp = 44.dp, radius: Dp = 14.dp, fontSize: Int = 15) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(radius))
            .background(Ink),
        contentAlignment = Alignment.Center,
    ) {
        Text("밥", style = GomType.numeral.copy(color = Cream, fontSize = fontSize.sp, lineHeight = fontSize.sp))
    }
}

/** 44dp 흰 카드형 아이콘 버튼(1.5dp Sand 테두리, radius 14). */
@Composable
fun CardIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Ink,
    background: Color = Color.White,
    border: Color = Sand,
    size: Dp = 44.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .border(1.5.dp, border, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(22.dp))
    }
}

/** 채운 CTA 버튼. */
@Composable
fun FilledCta(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    background: Color = Paprika,
    contentColor: Color = Cream,
    height: Dp = 52.dp,
    radius: Dp = 16.dp,
    shadow: Boolean = false,
    content: @Composable RowScope.() -> Unit,
) {
    val shape = RoundedCornerShape(radius)
    Row(
        modifier = modifier
            .height(height)
            .then(if (shadow && enabled) Modifier.shadow(12.dp, shape, ambientColor = Paprika, spotColor = Paprika) else Modifier)
            .clip(shape)
            .background(if (enabled) background else background.copy(alpha = 0.35f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides contentColor,
            androidx.compose.material3.LocalTextStyle provides GomType.button.copy(color = contentColor),
        ) { content() }
    }
}

/** 테두리형 보조 버튼(White, 1.5 Sand). */
@Composable
fun OutlinedCta(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 48.dp,
    radius: Dp = 14.dp,
    background: Color = Color.White,
    contentColor: Color = Ink,
    border: Color = Sand,
    content: @Composable RowScope.() -> Unit,
) {
    val shape = RoundedCornerShape(radius)
    Row(
        modifier = modifier
            .height(height)
            .clip(shape)
            .background(background)
            .border(BorderStroke(1.5.dp, border), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides contentColor,
            androidx.compose.material3.LocalTextStyle provides GomType.button.copy(color = contentColor),
        ) { content() }
    }
}

/** 바텀시트 핸들 36×4 SandDeep */
@Composable
fun SheetHandle(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(top = 10.dp, bottom = 6.dp)
            .width(36.dp)
            .height(4.dp)
            .clip(CircleShape)
            .background(SandDeep),
    )
}

/** 별 5개 표시(읽기 전용). */
@Composable
fun RatingStars(rating: Int, starSize: Dp = 16.dp, gap: Dp = 1.dp, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(gap)) {
        repeat(5) { i ->
            val filled = i < rating
            Icon(
                imageVector = if (filled) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                contentDescription = null,
                tint = if (filled) Paprika else SandDeep,
                modifier = Modifier.size(starSize),
            )
        }
    }
}

/** 한 줄 말줄임 텍스트 */
@Composable
fun EllipsisText(text: String, style: androidx.compose.ui.text.TextStyle, modifier: Modifier = Modifier, maxLines: Int = 1) {
    Text(text, style = style, maxLines = maxLines, overflow = TextOverflow.Ellipsis, modifier = modifier)
}

/** AdMob 320×50 배너. 룰렛 결과 상태에서만 사용. */
@Composable
fun AdBanner(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth().height(50.dp),
        shape = RoundedCornerShape(12.dp),
        color = Cream.copy(alpha = 0.08f),
    ) {
        Box(contentAlignment = Alignment.Center) {
            AndroidView(
                factory = { context ->
                    AdView(context).apply {
                        setAdSize(AdSize.BANNER)
                        adUnitId = BuildConfig.ADMOB_BANNER_ID
                        loadAd(AdRequest.Builder().build())
                    }
                },
            )
        }
    }
}

