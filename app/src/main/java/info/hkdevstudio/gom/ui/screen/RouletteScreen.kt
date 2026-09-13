package info.hkdevstudio.gom.ui.screen

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import info.hkdevstudio.gom.domain.Place
import info.hkdevstudio.gom.ui.MainViewModel
import info.hkdevstudio.gom.ui.RouletteSource
import info.hkdevstudio.gom.ui.theme.Cream
import info.hkdevstudio.gom.ui.theme.GomType
import info.hkdevstudio.gom.ui.theme.Ink
import info.hkdevstudio.gom.ui.theme.Ink2
import info.hkdevstudio.gom.ui.theme.Mute
import info.hkdevstudio.gom.ui.theme.Paprika
import info.hkdevstudio.gom.ui.theme.PaprikaDeep
import info.hkdevstudio.gom.ui.theme.PaprikaLight
import info.hkdevstudio.gom.ui.theme.PaprikaTint
import info.hkdevstudio.gom.ui.theme.RoulettePalette
import info.hkdevstudio.gom.ui.theme.Sand
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.random.Random

private const val SPIN_MS_PER_REV = 900
private const val MIN_SPIN_MS = 1_500L
private const val DECEL_MS = 3_000

/** S2(회전 중) / S3(결과). 전면 화면. */
@Composable
fun RouletteScreen(
    viewModel: MainViewModel,
    onClose: () -> Unit,
    onShowOnMap: (Place) -> Unit,
    onDecide: (Place) -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val visits by viewModel.visits.collectAsState()
    val candidates = state.rouletteCandidates
    val haptic = LocalHapticFeedback.current

    var winner by remember { mutableStateOf<Place?>(null) }
    var spinKey by remember { mutableIntStateOf(0) }
    var skip by remember { mutableStateOf(false) }
    var showCandidateSheet by remember { mutableStateOf(false) }
    val rotation = remember { Animatable(0f) }

    LaunchedEffect(candidates.isEmpty()) { if (candidates.isEmpty()) onClose() }

    // 회전 → 감속 정지. 탭(skip)이면 즉시 최종 각도로.
    LaunchedEffect(spinKey, candidates) {
        if (candidates.isEmpty()) return@LaunchedEffect
        winner = null
        skip = false
        val index = Random.nextInt(candidates.size)
        val sector = 360f / candidates.size
        val finalAngle = 360f - (index * sector + sector / 2f)

        rotation.snapTo(0f)
        val spinJob = launch {
            while (true) rotation.animateTo(rotation.value + 360f, tween(SPIN_MS_PER_REV, easing = LinearEasing))
        }
        withTimeoutOrNull(MIN_SPIN_MS) { snapshotFlow { skip }.first { it } }
        spinJob.cancel()
        spinJob.join()

        val target = (ceil(rotation.value / 360f) + 2f) * 360f + finalAngle
        if (!skip) {
            val decel = launch {
                rotation.animateTo(target, tween(DECEL_MS, easing = CubicBezierEasing(0.1f, 0.75f, 0.15f, 1f)))
            }
            val skipWatch = launch { snapshotFlow { skip }.first { it }; decel.cancel() }
            decel.join()
            skipWatch.cancel()
        }
        rotation.snapTo(target)
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        winner = candidates[index]
    }

    // 섹터 경계 통과 시 light tick
    LaunchedEffect(candidates.size) {
        if (candidates.isEmpty()) return@LaunchedEffect
        val sector = 360f / candidates.size
        snapshotFlow { floor(((rotation.value % 360f) + 360f) % 360f / sector).toInt() }
            .distinctUntilChanged()
            .collect { if (winner == null) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
    }

    val spinning = winner == null
    val excludedLabel = state.excludedCategories.take(2).joinToString("·")
    val chipText = when {
        state.rouletteSource == RouletteSource.Favorites -> "즐겨찾기 ${candidates.size}곳"
        excludedLabel.isNotBlank() -> "후보 ${candidates.size}곳 · $excludedLabel 빼고"
        else -> "후보 ${candidates.size}곳"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink)
            .then(
                if (spinning) Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { skip = true } else Modifier
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            // 상단 바
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Close, contentDescription = "닫기", tint = Cream) }
                Spacer(Modifier.weight(1f))
                Row(
                    modifier = Modifier
                        .height(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Cream.copy(alpha = 0.12f))
                        .clickable(enabled = state.rouletteSource == RouletteSource.Nearby) { showCandidateSheet = true }
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (state.rouletteSource == RouletteSource.Nearby) {
                        Icon(Icons.Rounded.Tune, contentDescription = null, tint = Cream, modifier = Modifier.size(16.dp))
                    }
                    Text(chipText, style = GomType.bodyS.copy(color = Cream))
                }
            }

            // 제목
            Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp)) {
                Text(
                    buildAnnotatedString {
                        withStyle(GomType.displayTitle.copy(color = Cream).toSpanStyle()) {
                            append(if (spinning) "두구두구 " else "두구두구…\n")
                        }
                        withStyle(GomType.displayTitle.copy(color = PaprikaLight).toSpanStyle()) {
                            append(if (spinning) "두구…" else "오늘 점심은")
                        }
                    },
                    style = GomType.displayTitle,
                )
                if (spinning) {
                    Spacer(Modifier.height(6.dp))
                    Text("김대리가 고민 중입니다", style = GomType.body.copy(color = Cream.copy(alpha = 0.6f)))
                }
            }

            // 휠
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                contentAlignment = Alignment.TopCenter,
            ) {
                RouletteWheel(
                    names = candidates.map { it.name },
                    rotationDegrees = rotation.value,
                    modifier = Modifier.size(320.dp),
                )
                // 포인터: 상단 중앙 30×32 Cream 역삼각형
                Canvas(modifier = Modifier.width(30.dp).height(32.dp).offset(y = (-6).dp)) {
                    val path = Path().apply {
                        moveTo(0f, 0f); lineTo(size.width, 0f); lineTo(size.width / 2f, size.height); close()
                    }
                    drawPath(path, Cream)
                }
            }

            Spacer(Modifier.weight(1f))

            val result = winner
            if (result == null) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Cream.copy(alpha = 0.08f)),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.HourglassTop, contentDescription = null, tint = Cream.copy(alpha = 0.45f), modifier = Modifier.size(20.dp))
                    Text("돌아가는 중…", style = GomType.button.copy(color = Cream.copy(alpha = 0.45f)))
                }
                Text(
                    "화면을 탭하면 바로 멈춥니다",
                    style = GomType.meta.copy(color = Cream.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 16.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            } else {
                val placeVisits = visits.filter { it.placeId == result.id }
                val rated = placeVisits.filter { it.rating > 0 }
                val avg = if (rated.isEmpty()) null else rated.map { it.rating }.average()
                ResultCard(
                    place = result,
                    visitCount = placeVisits.size,
                    avgRating = avg,
                    onRetry = { spinKey++ },
                    onShowOnMap = { onShowOnMap(result) },
                    onDecide = { onDecide(result) },
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                AdBanner(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp))
            }
        }
    }

    if (showCandidateSheet) {
        CandidateSheet(
            viewModel = viewModel,
            onStart = {
                showCandidateSheet = false
                viewModel.refreshRouletteCandidates()
                spinKey++
            },
            onDismiss = {
                showCandidateSheet = false
                viewModel.refreshRouletteCandidates()
            },
        )
    }
}

@Composable
private fun ResultCard(
    place: Place,
    visitCount: Int,
    avgRating: Double?,
    onRetry: () -> Unit,
    onShowOnMap: () -> Unit,
    onDecide: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Cream)
                .padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(place.name, style = GomType.displayS, maxLines = 2)
                    val meta = buildString {
                        append(place.metaLine())
                        if (avgRating != null) append(" · ★ ${"%.1f".format(avgRating)} (내 별점)")
                    }
                    Text(meta, style = GomType.bodyS.copy(color = Mute))
                }
                Box(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(PaprikaTint)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(if (visitCount == 0) "처음 가는 곳" else "${visitCount + 1}번째 방문", style = GomType.badge)
                }
            }
            Canvas(modifier = Modifier.fillMaxWidth().height(1.5.dp)) {
                drawLine(
                    Sand, Offset(0f, size.height / 2), Offset(size.width, size.height / 2),
                    strokeWidth = size.height,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedCta(onClick = onRetry, background = Cream) {
                    Icon(Icons.Rounded.Replay, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("다시")
                }
                OutlinedCta(onClick = onShowOnMap, background = Cream, modifier = Modifier.width(48.dp)) {
                    Icon(Icons.Rounded.Map, contentDescription = "지도에서 보기", modifier = Modifier.size(20.dp))
                }
                FilledCta(onClick = onDecide, height = 48.dp, radius = 14.dp, modifier = Modifier.weight(1f)) {
                    Text("여기로 결정")
                }
            }
        }
        // 티켓 펀치 홀
        Box(
            modifier = Modifier
                .offset(x = (-8).dp, y = 64.dp)
                .size(16.dp)
                .clip(CircleShape)
                .background(Ink),
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 8.dp, y = 64.dp)
                .size(16.dp)
                .clip(CircleShape)
                .background(Ink),
        )
    }
}

/** 룰렛 휠: conic 섹터 + 8dp Ink2 외곽 링 + 72dp Cream 허브("밥"은 회전하지 않음). */
@Composable
fun RouletteWheel(
    names: List<String>,
    rotationDegrees: Float,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val labelPaint = remember(density) {
        android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = with(density) { 13.sp.toPx() }
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
            isFakeBoldText = true
        }
    }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .shadow(30.dp, CircleShape, clip = false, ambientColor = Color.Black, spotColor = Color.Black)
                .clip(CircleShape),
        ) {
            val radius = size.minDimension / 2f
            val ring = 8.dp.toPx()
            drawCircle(Ink2, radius = radius, center = size.center)
            if (names.isEmpty()) return@Canvas
            val sector = 360f / names.size
            val inner = radius - ring
            rotate(rotationDegrees, pivot = size.center) {
                names.forEachIndexed { index, name ->
                    drawArc(
                        color = RoulettePalette[index % RoulettePalette.size],
                        startAngle = index * sector - 90f,
                        sweepAngle = sector,
                        useCenter = true,
                        topLeft = Offset(size.center.x - inner, size.center.y - inner),
                        size = androidx.compose.ui.geometry.Size(inner * 2, inner * 2),
                    )
                    val textAngle = index * sector + sector / 2f
                    rotate(textAngle, pivot = size.center) {
                        drawContext.canvas.nativeCanvas.drawText(
                            if (name.length > 8) name.take(7) + "…" else name,
                            size.center.x,
                            size.center.y - inner * 0.66f,
                            labelPaint,
                        )
                    }
                }
            }
        }
        // 허브
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Cream),
            contentAlignment = Alignment.Center,
        ) { Text("밥", style = GomType.numeral.copy(color = Ink)) }
    }
}
