package kr.ai.redbridgedev.bobmap.ui.screen

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Comment
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.kakao.vectormap.KakaoMap
import kr.ai.redbridgedev.bobmap.BuildConfig
import kr.ai.redbridgedev.bobmap.data.local.VisitEntity
import kr.ai.redbridgedev.bobmap.domain.Place
import kr.ai.redbridgedev.bobmap.ui.MainViewModel
import kr.ai.redbridgedev.bobmap.ui.theme.Cream
import kr.ai.redbridgedev.bobmap.ui.theme.CreamMap
import kr.ai.redbridgedev.bobmap.ui.theme.GomType
import kr.ai.redbridgedev.bobmap.ui.theme.Ink
import kr.ai.redbridgedev.bobmap.ui.theme.Mute
import kr.ai.redbridgedev.bobmap.ui.theme.Mute2
import kr.ai.redbridgedev.bobmap.ui.theme.Paprika
import kr.ai.redbridgedev.bobmap.ui.theme.PaprikaDeep
import kr.ai.redbridgedev.bobmap.ui.theme.PaprikaTint
import kr.ai.redbridgedev.bobmap.ui.theme.PaprikaTintBorder
import kr.ai.redbridgedev.bobmap.ui.theme.Sand
import kr.ai.redbridgedev.bobmap.util.GeoUtils
import kr.ai.redbridgedev.bobmap.util.ShareUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * S7. 매장 정보(전면 화면).
 * @param pendingVisitId 룰렛 "여기로 결정"으로 방금 생성된 방문 기록 → 진입 즉시 별점 시트
 */
@Composable
fun PlaceDetailScreen(
    viewModel: MainViewModel,
    placeId: String,
    pendingVisitId: Long?,
    onBack: () -> Unit,
    onOpenRecords: () -> Unit,
    onOpenKakaoPage: (Place) -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val excludedPlaces by viewModel.excludedPlaces.collectAsState()
    val visits by viewModel.visits.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val place = remember(placeId, state.places, favorites) { viewModel.placeById(placeId) }
    if (place == null) {
        Box(modifier = Modifier.fillMaxSize().background(Cream), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("가게 정보를 찾을 수 없어요", style = GomType.titleM)
                OutlinedCta(onClick = onBack) { Text("돌아가기") }
            }
        }
        return
    }

    val placeVisits = visits.filter { it.placeId == place.id }
    val rated = placeVisits.filter { it.rating > 0 }
    val avg = if (rated.isEmpty()) null else rated.map { it.rating }.average()
    val isFavorite = favorites.any { it.id == place.id }
    val isExcluded = excludedPlaces.any { it.id == place.id }

    // 별점 시트 대상: null = 닫힘, -1 = 새 기록, 그 외 = 기존 visitId
    var ratingTarget by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(pendingVisitId) { if (pendingVisitId != null && pendingVisitId > 0) ratingTarget = pendingVisitId }

    var heartPulse by remember { mutableStateOf(false) }
    val heartScale by animateFloatAsState(
        targetValue = if (heartPulse) 1.2f else 1f,
        animationSpec = keyframes { durationMillis = 150; 1.2f at 75; 1f at 150 },
        finishedListener = { heartPulse = false },
        label = "heart",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
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
            PlainIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "뒤로", onClick = onBack)
            Spacer(Modifier.weight(1f))
            Box(modifier = Modifier.scale(heartScale)) {
                PlainIconButton(
                    icon = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = "즐겨찾기",
                    tint = if (isFavorite) Paprika else Ink,
                    onClick = { heartPulse = true; viewModel.toggleFavorite(place) },
                )
            }
            PlainIconButton(Icons.Rounded.Share, "공유") {
                // 최근 별점·메모를 붙여 추천처럼 공유
                val latest = placeVisits.firstOrNull { it.rating > 0 || it.note.isNotBlank() }
                ShareUtils.sharePlace(context, place, latest?.rating ?: 0, latest?.note.orEmpty())
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            // 헤더
            Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val depth3 = place.fullCategory.split(">").map { it.trim() }.getOrNull(2)
                Text(
                    listOfNotNull(place.category.ifBlank { null }, depth3, place.distanceM?.let { "내 위치에서 ${it}m" })
                        .joinToString(" · "),
                    style = GomType.bodyS.copy(color = PaprikaDeep),
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(place.name, style = GomType.displayM, modifier = Modifier.weight(1f))
                    PlaceThumb(place = place, size = 72.dp, radius = 16.dp)
                }
                val phone = place.phone.ifBlank { null }
                Text(
                    listOfNotNull(place.address.ifBlank { null }, phone).joinToString("\n"),
                    style = GomType.body.copy(color = Mute),
                    modifier = if (phone != null) Modifier.clickable {
                        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
                    } else Modifier,
                )
            }

            // 통계 3열
            Row(
                modifier = Modifier
                    .padding(start = 24.dp, end = 24.dp, top = 20.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StatCard(value = "${placeVisits.size}번", label = if (placeVisits.isEmpty()) "아직" else "다녀옴", modifier = Modifier.weight(1f))
                StatCard(value = if (avg == null) "★ –" else "★ ${"%.1f".format(avg)}", label = "내 평균", modifier = Modifier.weight(1f))
                StatCard(value = place.distanceM?.let { "${GeoUtils.walkMinutes(it)}분" } ?: "–", label = "도보", modifier = Modifier.weight(1f))
            }

            // 미니 지도
            Box(
                modifier = Modifier
                    .padding(start = 24.dp, end = 24.dp, top = 16.dp)
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(CreamMap)
                    .border(1.5.dp, Sand, RoundedCornerShape(18.dp)),
            ) {
                if (BuildConfig.KAKAO_NATIVE_APP_KEY.isNotBlank() && place.lat != 0.0) {
                    val density = LocalDensity.current.density
                    KakaoMapView(
                        initialLat = place.lat,
                        initialLng = place.lng,
                        zoom = 16,
                        interactive = false,
                        onMapReady = { map: KakaoMap ->
                            map.renderPlaceLabels(listOf(place), setOf(place.id), density, me = null)
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 12.dp, bottom = 10.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Ink)
                        .clickable {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(kakaoMapUrl(place))))
                        }
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text("카카오맵에서 보기", style = GomType.meta.copy(color = Cream))
                    Icon(Icons.Rounded.OpenInNew, contentDescription = null, tint = Cream, modifier = Modifier.size(14.dp))
                }
            }

            // 카카오 리뷰·사진 (플레이스 페이지를 앱 안 WebView로)
            OutlinedCta(
                onClick = { onOpenKakaoPage(place) },
                height = 48.dp,
                radius = 14.dp,
                modifier = Modifier
                    .padding(start = 24.dp, end = 24.dp, top = 12.dp)
                    .fillMaxWidth(),
            ) {
                Icon(Icons.AutoMirrored.Rounded.Comment, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("카카오 리뷰 · 사진 보기")
            }

            // 내 한 줄 기록
            Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("내 한 줄 기록", style = GomType.titleS, modifier = Modifier.weight(1f))
                    if (placeVisits.size > 3) {
                        Text(
                            "전체 보기",
                            style = GomType.bodyS.copy(color = PaprikaDeep),
                            modifier = Modifier.clickable(onClick = onOpenRecords).padding(4.dp),
                        )
                    }
                }
                if (placeVisits.isEmpty()) {
                    Text("아직 기록이 없어요. 다녀오면 한 줄 남겨보세요.", style = GomType.bodyS.copy(color = Mute2), modifier = Modifier.padding(top = 8.dp))
                }
                placeVisits.take(3).forEach { visit ->
                    VisitRow(visit = visit, onClick = { ratingTarget = visit.visitId })
                }
            }
        }

        // 하단 CTA
        Row(
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedCta(
                onClick = { viewModel.toggleExcludePlace(place) },
                height = 52.dp,
                radius = 16.dp,
                background = if (isExcluded) PaprikaTint else Color.White,
                border = if (isExcluded) PaprikaTintBorder else Sand,
                contentColor = if (isExcluded) PaprikaDeep else Ink,
            ) {
                Icon(Icons.Rounded.Block, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(if (isExcluded) "빼둠 · 해제" else "당분간 빼기")
            }
            FilledCta(
                onClick = { ratingTarget = -1L },
                background = Ink,
                contentColor = Cream,
                height = 52.dp,
                radius = 16.dp,
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Rounded.Restaurant, contentDescription = null, modifier = Modifier.size(20.dp))
                Text("다녀왔어요 · 별점 남기기")
            }
        }
    }

    ratingTarget?.let { target ->
        val existing = if (target > 0) placeVisits.find { it.visitId == target } else null
        val visitNumber = if (existing != null) {
            placeVisits.count { it.visitedAt <= existing.visitedAt }
        } else placeVisits.size + 1
        RatingSheet(
            placeName = place.name,
            visitedAt = existing?.visitedAt ?: System.currentTimeMillis(),
            visitNumber = visitNumber,
            initialRating = existing?.rating ?: 0,
            initialNote = existing?.note.orEmpty(),
            onSave = { rating, note ->
                scope.launch {
                    if (target > 0) viewModel.updateVisit(target, rating, note)
                    else viewModel.recordVisit(place, rating, note)
                }
                ratingTarget = null
            },
            // 취소: 새 기록이면 아무것도 남기지 않음. 룰렛에서 자동 생성된 기록이면 되돌림(삭제)
            onCancel = {
                if (existing != null && pendingVisitId == target) viewModel.deleteVisit(existing)
                ratingTarget = null
            },
            // 나중에: 별점 없이 방문만 남김(룰렛 자동 기록은 이미 존재하므로 그대로 둠)
            onLater = when {
                target < 0 -> { { scope.launch { viewModel.recordVisit(place) }; ratingTarget = null } }
                pendingVisitId == target -> { { ratingTarget = null } }
                else -> null
            },
            onDelete = if (existing != null && pendingVisitId != target) {
                { viewModel.deleteVisit(existing); ratingTarget = null }
            } else null,
        )
    }
}

fun kakaoMapUrl(place: Place): String = "https://place.map.kakao.com/${place.id}"

@Composable
private fun PlainIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String?,
    tint: Color = Ink,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(24.dp)) }
}

@Composable
private fun StatCard(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.5.dp, Sand, RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(value, style = GomType.numeralL, maxLines = 1)
        Text(label, style = GomType.meta)
    }
}

@Composable
fun VisitRow(visit: VisitEntity, onClick: () -> Unit) {
    val date = remember(visit.visitedAt) { SimpleDateFormat("M월 d일", Locale.KOREAN).format(Date(visit.visitedAt)) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(date, style = GomType.meta, modifier = Modifier.width(64.dp))
        RatingStars(rating = visit.rating)
        EllipsisText(
            text = visit.note.ifBlank { "한 줄 남기기…" },
            style = GomType.bodyS.copy(color = if (visit.note.isBlank()) Mute2 else Ink),
            modifier = Modifier.weight(1f),
        )
    }
}
