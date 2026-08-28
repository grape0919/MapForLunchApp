package info.hkdevstudio.gom.ui.screen

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import info.hkdevstudio.gom.domain.Place
import kotlin.random.Random

private val WheelColors = listOf(
    Color(0xFF0E9B82), Color(0xFF3CC9AD), Color(0xFF0B7A67), Color(0xFF63D8C2),
    Color(0xFF12876F), Color(0xFF2AB598), Color(0xFF0A6B5B), Color(0xFF4ACFB5),
)

/**
 * "골라줘 내 점심" 룰렛. 당첨자를 먼저 정하고, 그 자리에서 멈추도록 회전 각을 계산한다.
 */
@Composable
fun RouletteDialog(
    candidates: List<Place>,
    onDecide: (Place) -> Unit,
    onShowOnMap: (Place) -> Unit,
    onDismiss: () -> Unit,
) {
    var winner by remember { mutableStateOf<Place?>(null) }
    var spinKey by remember { mutableStateOf(0) }
    val rotation = remember { Animatable(0f) }

    LaunchedEffect(spinKey) {
        winner = null
        val winnerIndex = Random.nextInt(candidates.size)
        val sector = 360f / candidates.size
        // 포인터(12시 방향)에 당첨 섹터 중앙이 오도록 회전
        val target = 5 * 360f + (360f - (winnerIndex * sector + sector / 2f))
        rotation.snapTo(0f)
        rotation.animateTo(
            targetValue = target,
            animationSpec = tween(durationMillis = 3600, easing = CubicBezierEasing(0.15f, 0.6f, 0.15f, 1f)),
        )
        winner = candidates[winnerIndex]
    }

    Dialog(onDismissRequest = onDismiss) {
        Card {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "골라줘 내 점심 🎯",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )

                Box(contentAlignment = Alignment.TopCenter) {
                    RouletteWheel(
                        names = candidates.map { it.name },
                        rotationDegrees = rotation.value,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .padding(top = 10.dp),
                    )
                    // 포인터
                    Canvas(modifier = Modifier.size(24.dp)) {
                        val path = androidx.compose.ui.graphics.Path().apply {
                            moveTo(0f, 0f)
                            lineTo(size.width, 0f)
                            lineTo(size.width / 2f, size.height)
                            close()
                        }
                        drawPath(path, Color(0xFFB3402F))
                    }
                }

                val result = winner
                if (result != null) {
                    Text(
                        text = result.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = listOfNotNull(
                            result.category.ifBlank { null },
                            result.distanceM?.let { "${it}m" },
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { spinKey++ }) { Text("다시!") }
                        OutlinedButton(onClick = { onShowOnMap(result) }) { Text("지도에서 보기") }
                        Button(onClick = { onDecide(result) }) { Text("여기로 결정") }
                    }
                } else {
                    Text(
                        text = "두구두구두구…",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }

                TextButton(onClick = onDismiss) { Text("닫기") }
            }
        }
    }
}

@Composable
private fun RouletteWheel(
    names: List<String>,
    rotationDegrees: Float,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val sector = 360f / names.size
        val radius = size.minDimension / 2f
        rotate(rotationDegrees, pivot = size.center) {
            names.forEachIndexed { index, name ->
                drawArc(
                    color = WheelColors[index % WheelColors.size],
                    startAngle = index * sector - 90f,
                    sweepAngle = sector,
                    useCenter = true,
                )
                // 섹터 중앙 각도에 가게 이름
                val textAngle = index * sector + sector / 2f - 90f
                rotate(textAngle + 90f, pivot = size.center) {
                    drawContext.canvas.nativeCanvas.drawText(
                        if (name.length > 8) name.take(7) + "…" else name,
                        size.center.x,
                        size.center.y - radius * 0.62f,
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.WHITE
                            textSize = 13.sp.toPx()
                            textAlign = android.graphics.Paint.Align.CENTER
                            isAntiAlias = true
                            isFakeBoldText = true
                        },
                    )
                }
            }
        }
        drawCircle(Color.White, radius = radius * 0.16f, center = size.center)
    }
}
