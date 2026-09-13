package info.hkdevstudio.gom.ui.screen

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.StopCircle
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
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
import info.hkdevstudio.gom.ui.theme.PaprikaLight
import info.hkdevstudio.gom.ui.theme.PaprikaTint
import info.hkdevstudio.gom.ui.theme.RoulettePalette
import info.hkdevstudio.gom.ui.theme.Sand
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.random.Random

private const val SPIN_MS_PER_REV = 900
private const val DECEL_MS = 3_000

private sealed interface Phase {
    data object Idle : Phase
    data object Spinning : Phase
    data class Result(val place: Place) : Phase
}

/**
 * S2(대기 → 회전 중) / S3(결과). 전면 화면.
 * 흐름: 돌림판 확인·후보 빼기 → "돌리기" → 회전 → "멈추기"(또는 화면 탭) → 결과.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RouletteScreen(
    viewModel: MainViewModel,
    onClose: () -> Unit,
    onShowOnMap: (Place) -> Unit,
    onDecide: (Place) -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val visits by viewModel.visits.collectAsState()
    val wheel = state.rouletteWheel
    val haptic = LocalHapticFeedback.current

    var phase by remember { mutableStateOf<Phase>(Phase.Idle) }
    var stopRequested by remember { mutableStateOf(false) }
    var showCandidateSheet by remember { mutableStateOf(false) }
    val rotation = remember { Animatable(0f) }

    LaunchedEffect(wheel.isEmpty()) { if (wheel.isEmpty()) onClose() }

    // 회전: "멈추기"/탭 전까지 무한 회전 → 감속 정지
    LaunchedEffect(phase) {
        if (phase != Phase.Spinning || wheel.isEmpty()) return@LaunchedEffect
        stopRequested = false
        val index = Random.nextInt(wheel.size)
        val sector = 360f / wheel.size
        val finalAngle = 360f - (index * sector + sector / 2f)

        val spinJob = launch {
            while (true) rotation.animateTo(rotation.value + 360f, tween(SPIN_MS_PER_REV, easing = LinearEasing))
        }
        snapshotFlow { stopRequested }.first { it }
        spinJob.cancel()
        spinJob.join()

        val target = (ceil(rotation.value / 360f) + 2f) * 360f + finalAngle
        rotation.animateTo(target, tween(DECEL_MS, easing = CubicBezierEasing(0.1f, 0.75f, 0.15f, 1f)))
        rotation.snapTo(target)
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        phase = Phase.Result(wheel[index])
    }

    // 섹터 경계 통과 시 light tick
    LaunchedEffect(wheel.size) {
        if (wheel.isEmpty()) return@LaunchedEffect
        val sector = 360f / wheel.size
        snapshotFlow { floor(((rotation.value % 360f) + 360f) % 360f / sector).toInt() }
            .distinctUntilChanged()
            .collect { if (phase == Phase.Spinning) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
    }

    val spinning = phase == Phase.Spinning
    val excludedLabel = state.excludedCategories.take(2).joinToString("·")
    val poolText = if (state.rouletteSource == RouletteSource.Favorites) "즐겨찾기 ${state.roulettePool.size}곳" else "후보 ${state.roulettePool.size}곳"
    val chipText = buildString {
        append(poolText)
        if (state.roulettePool.size > wheel.size) append(" · 이번 판 ${wheel.size}")
        if (excludedLabel.isNotBlank()) append(" · $excludedLabel 빼고")
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink)
            .then(
                if (spinning) Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { stopRequested = true } else Modifier
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
                        .clickable(enabled = !spinning) { showCandidateSheet = true }
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(Icons.Rounded.Tune, contentDescription = null, tint = Cream, modifier = Modifier.size(16.dp))
                    Text(chipText, style = GomType.bodyS.copy(color = Cream), maxLines = 1)
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                // 제목
                Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 12.dp)) {
                    val (head, accent, sub) = when (phase) {
                        Phase.Idle -> Triple("오늘 점심, ", "돌려볼까요?", "돌림판의 가게를 눌러 이번 판에서 뺄 수 있어요")
                        Phase.Spinning -> Triple("두구두구 ", "두구…", "김대리가 고민 중입니다")
                        is Phase.Result -> Triple("두구두구…\n", "오늘 점심은", null)
                    }
                    Text(
                        buildAnnotatedString {
                            withStyle(GomType.displayTitle.copy(color = Cream).toSpanStyle()) { append(head) }
                            withStyle(GomType.displayTitle.copy(color = PaprikaLight).toSpanStyle()) { append(accent) }
                        },
                        style = GomType.displayTitle,
                    )
                    if (sub != null) {
                        Spacer(Modifier.height(6.dp))
                        Text(sub, style = GomType.body.copy(color = Cream.copy(alpha = 0.6f)))
                    }
                }

                // 휠
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    RouletteWheel(
                        names = wheel.map { it.name },
                        rotationDegrees = rotation.value,
                        modifier = Modifier.size(300.dp),
                    )
                    Canvas(modifier = Modifier.width(30.dp).height(32.dp).offset(y = (-6).dp)) {
                        val path = Path().apply {
                            moveTo(0f, 0f); lineTo(size.width, 0f); lineTo(size.width / 2f, size.height); close()
                        }
                        drawPath(path, Cream)
                    }
                }

                // 대기 상태: 이번 판 후보 칩(탭 → 당분간 빼기)
                if (phase == Phase.Idle) {
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        wheel.forEachIndexed { i, place ->
                            Row(
                                modifier = Modifier
                                    .height(32.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Cream.copy(alpha = 0.08f))
                                    .border(1.dp, Cream.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                                    .clickable { viewModel.toggleExcludePlace(place) }
                                    .padding(start = 8.dp, end = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Box(
                                    modifier = Modifier.size(10.dp).clip(CircleShape)
                                        .background(RoulettePalette[i % RoulettePalette.size]),
                                )
                                Text(place.name, style = GomType.bodyS.copy(color = Cream), maxLines = 1)
                                Icon(Icons.Rounded.Block, contentDescription = "당분간 빼기", tint = Cream.copy(alpha = 0.5f), modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // 하단
            when (val p = phase) {
                Phase.Idle -> {
                    FilledCta(
                        onClick = { phase = Phase.Spinning },
                        height = 60.dp,
                        radius = 18.dp,
                        shadow = true,
                        modifier = Modifier
                            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                            .fillMaxWidth(),
                    ) {
                        Icon(Icons.Rounded.Casino, contentDescription = null, modifier = Modifier.size(26.dp))
                        Text("돌림판 돌리기", style = GomType.numeral.copy(color = Cream, fontSize = 22.sp))
                    }
                }
                Phase.Spinning -> {
                    FilledCta(
                        onClick = { stopRequested = true },
                        background = Cream,
                        contentColor = Ink,
                        height = 56.dp,
                        radius = 16.dp,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth(),
                    ) {
                        Icon(Icons.Rounded.StopCircle, contentDescription = null, modifier = Modifier.size(22.dp))
                        Text("멈추기")
                    }
                    Text(
                        "화면을 탭해도 멈춥니다",
                        style = GomType.meta.copy(color = Cream.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp, bottom = 16.dp),
                        textAlign = TextAlign.Center,
                    )
                }
                is Phase.Result -> {
                    val result = p.place
                    val placeVisits = visits.filter { it.placeId == result.id }
                    val rated = placeVisits.filter { it.rating > 0 }
                    val avg = if (rated.isEmpty()) null else rated.map { it.rating }.average()
                    ResultCard(
                        place = result,
                        visitCount = placeVisits.size,
                        avgRating = avg,
                        onRetry = {
                            viewModel.reshuffleWheel()
                            phase = Phase.Idle
                        },
                        onShowOnMap = { onShowOnMap(result) },
                        onDecide = { onDecide(result) },
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    AdBanner(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp))
                }
            }
        }
    }

    if (showCandidateSheet) {
        CandidateSheet(
            viewModel = viewModel,
            onStart = {
                showCandidateSheet = false
                viewModel.refreshRouletteCandidates()
                phase = Phase.Spinning
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
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Cream),
            contentAlignment = Alignment.Center,
        ) { Text("밥", style = GomType.numeral.copy(color = Ink)) }
    }
}
