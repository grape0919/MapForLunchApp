package kr.ai.redbridgedev.bobmap.ui.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kr.ai.redbridgedev.bobmap.ui.theme.Cream
import kr.ai.redbridgedev.bobmap.ui.theme.GomType
import kr.ai.redbridgedev.bobmap.ui.theme.Ink
import kr.ai.redbridgedev.bobmap.ui.theme.Paprika
import kr.ai.redbridgedev.bobmap.ui.theme.PaprikaLight
import kr.ai.redbridgedev.bobmap.ui.theme.RoulettePalette

/** S10. 첫 실행 / 위치 권한. */
@Composable
fun OnboardingScreen(
    onStartWithLocation: () -> Unit,
    onStartAtDefault: () -> Unit,
) {
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { onStartWithLocation() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Paprika),
    ) {
        // 장식: 좌측 룰렛 팔레트 conic 원(35% 알파, 22° 회전), 우상단 PaprikaLight 원
        Canvas(
            modifier = Modifier
                .size(320.dp)
                .offset(x = (-60).dp, y = 120.dp)
                .rotate(22f),
        ) {
            drawCircle(
                brush = Brush.sweepGradient(RoulettePalette.map { it.copy(alpha = 0.35f) } + RoulettePalette.first().copy(alpha = 0.35f)),
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 90.dp, y = (-70).dp)
                .size(260.dp)
                .clip(RoundedCornerShape(50))
                .background(PaprikaLight.copy(alpha = 0.5f)),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            Column(
                modifier = Modifier.padding(top = 72.dp, start = 28.dp, end = 28.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                LogoTile(size = 56.dp, radius = 18.dp)
                Text("오늘 점심,\n또 고민이죠?", style = GomType.displayL)
                Text(
                    "김대리가 회사 근처 맛집을 모아 룰렛으로 골라드립니다. 먼저 주변을 찾을 수 있게 위치를 허용해 주세요.",
                    style = GomType.body.copy(fontSize = 15.sp, lineHeight = 23.sp, color = Cream.copy(alpha = 0.9f)),
                )
            }

            Spacer(Modifier.weight(1f))

            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                FilledCta(
                    onClick = {
                        permissionLauncher.launch(
                            arrayOf(
                                android.Manifest.permission.ACCESS_FINE_LOCATION,
                                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                            )
                        )
                    },
                    background = Cream,
                    contentColor = Ink,
                    height = 56.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Rounded.MyLocation, contentDescription = null, modifier = Modifier.size(20.dp))
                    Text("내 위치로 시작")
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(onClick = onStartAtDefault),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "서울시청 근처로 둘러볼게요",
                        style = GomType.body.copy(color = Cream.copy(alpha = 0.85f), fontWeight = FontWeight.Medium),
                    )
                }
            }
        }
    }
}
