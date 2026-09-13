package info.hkdevstudio.gom.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kakao.vectormap.KakaoMap
import info.hkdevstudio.gom.BuildConfig
import info.hkdevstudio.gom.domain.Place
import info.hkdevstudio.gom.ui.MainViewModel
import info.hkdevstudio.gom.ui.theme.Cream
import info.hkdevstudio.gom.ui.theme.CreamMap
import info.hkdevstudio.gom.ui.theme.GomType
import info.hkdevstudio.gom.ui.theme.HeartOff
import info.hkdevstudio.gom.ui.theme.Ink
import info.hkdevstudio.gom.ui.theme.Mute
import info.hkdevstudio.gom.ui.theme.Mute2
import info.hkdevstudio.gom.ui.theme.Paprika
import info.hkdevstudio.gom.ui.theme.PaprikaDeep
import info.hkdevstudio.gom.ui.theme.PaprikaTint
import info.hkdevstudio.gom.ui.theme.Sand
import info.hkdevstudio.gom.util.GeoUtils
import info.hkdevstudio.gom.util.LocationProvider
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

private val CardWidth = 236.dp

/** S1. 지도 홈 — 번호 핀 + 하단 카드 캐러셀 + 룰렛 단일 CTA. */
@Composable
fun MapScreen(
    viewModel: MainViewModel,
    onOpenRecords: () -> Unit,
    onOpenRoulette: () -> Unit,
    onOpenPlace: (Place) -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val visits by viewModel.visits.collectAsState()
    val excludedPlaces by viewModel.excludedPlaces.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val density = LocalDensity.current.density
    val focusManager = LocalFocusManager.current

    var kakaoMap by remember { mutableStateOf<KakaoMap?>(null) }
    var showCandidateSheet by remember { mutableStateOf(false) }
    // 지도를 검색 중심에서 멀리 옮겼을 때만 "이 위치에서 재검색" 노출
    var mapMovedAway by remember { mutableStateOf(false) }

    val visible = remember(state.places, state.categoryFilter, state.favoritesOnly, state.favoriteIds) { state.visiblePlaces }
    val selectedIndex = visible.indexOfFirst { it.id == state.selectedPlaceId }.coerceAtLeast(0)
    val listState = rememberLazyListState()

    // 첫 진입 시 검색(권한 있으면 내 위치, 없으면 기본 좌표)
    LaunchedEffect(Unit) {
        if (!state.searched && !state.loading) {
            if (LocationProvider.hasPermission(context)) viewModel.refreshFromMyLocation()
            else viewModel.startAtDefaultLocation()
        }
    }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    // 핀 렌더링: 목록/선택 변경 시
    LaunchedEffect(kakaoMap, visible, selectedIndex, state.hasLocation, state.centerLat, state.centerLng, state.favoriteIds) {
        val map = kakaoMap ?: return@LaunchedEffect
        map.renderPlaceLabels(
            places = visible,
            activeIds = setOfNotNull(visible.getOrNull(selectedIndex)?.id),
            density = density,
            me = if (state.hasLocation) state.centerLat to state.centerLng else null,
            favoriteIds = state.favoriteIds,
        )
    }

    // 사용자가 지도를 끌어 검색 중심에서 벗어나면 재검색 버튼 노출
    LaunchedEffect(kakaoMap, state.centerLat, state.centerLng) {
        mapMovedAway = false
        kakaoMap?.setOnCameraMoveEndListener { _, position, gesture ->
            if (gesture == com.kakao.vectormap.GestureType.Unknown) return@setOnCameraMoveEndListener
            val d = GeoUtils.distanceMeters(state.centerLat, state.centerLng, position.position.latitude, position.position.longitude)
            mapMovedAway = d > 150
        }
    }

    // 검색 중심이 바뀌면 카메라 이동
    LaunchedEffect(kakaoMap, state.centerLat, state.centerLng) {
        kakaoMap?.animateTo(state.centerLat, state.centerLng, zoom = 16, durationMs = 300)
    }

    // 선택 카드 ↔ 카메라 + 캐러셀 위치 동기화
    LaunchedEffect(state.selectedPlaceId, kakaoMap) {
        val place = visible.getOrNull(selectedIndex) ?: return@LaunchedEffect
        kakaoMap?.animateTo(place.lat, place.lng, durationMs = 300)
        if (!listState.isScrollInProgress) {
            val centered = listState.layoutInfo.visibleItemsInfo.minByOrNull { info ->
                kotlin.math.abs(info.offset + info.size / 2 - listState.layoutInfo.viewportEndOffset / 2)
            }?.index
            if (centered != selectedIndex) listState.animateScrollToItem(selectedIndex)
        }
    }

    // 캐러셀 스크롤 정지 시 중앙 카드 = 선택
    LaunchedEffect(listState, visible) {
        snapshotFlow { listState.isScrollInProgress }
            .distinctUntilChanged()
            .filter { !it }
            .collect {
                val info = listState.layoutInfo
                val center = (info.viewportStartOffset + info.viewportEndOffset) / 2
                val nearest = info.visibleItemsInfo.minByOrNull { kotlin.math.abs(it.offset + it.size / 2 - center) }?.index
                    ?: return@collect
                visible.getOrNull(nearest)?.let { if (it.id != state.selectedPlaceId) viewModel.selectPlace(it.id) }
            }
    }

    val categoryCounts by remember(state.places) {
        derivedStateOf {
            state.places.filter { it.category.isNotBlank() }
                .groupingBy { it.category }.eachCount()
                .toList().sortedByDescending { it.second }
        }
    }
    val excludedCount = state.excludedCategories.size + excludedPlaces.size
    val candidateCount = state.eligiblePlaces.size

    Box(modifier = Modifier.fillMaxSize().background(CreamMap)) {
        if (BuildConfig.KAKAO_NATIVE_APP_KEY.isBlank()) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text(
                    "카카오 네이티브 앱 키가 없어 지도를 표시할 수 없습니다.\nlocal.properties 에 KAKAO_NATIVE_APP_KEY 를 설정하세요.",
                    style = GomType.body, textAlign = TextAlign.Center, modifier = Modifier.padding(32.dp),
                )
            }
        } else {
            KakaoMapView(
                initialLat = state.centerLat,
                initialLng = state.centerLng,
                onMapReady = { kakaoMap = it },
                onLabelClick = { id -> if (id != ME_TAG) viewModel.selectPlace(id) },
                modifier = Modifier.fillMaxSize(),
            )
        }

        // ---- 상단: 바 + 카테고리 칩 ----
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LogoTile()
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White)
                        .border(1.5.dp, Sand, RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Rounded.Search, contentDescription = "검색", tint = Mute, modifier = Modifier.size(20.dp))
                    BasicTextField(
                        value = state.keyword,
                        onValueChange = viewModel::onKeywordChange,
                        singleLine = true,
                        textStyle = GomType.body.copy(color = Ink),
                        cursorBrush = SolidColor(Paprika),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus(); viewModel.search() }),
                        modifier = Modifier.weight(1f),
                        decorationBox = { inner ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (state.keyword.isEmpty()) {
                                    Text(
                                        buildAnnotatedString {
                                            withStyle(GomType.body.copy(color = Ink).toSpanStyle()) { append("맛집") }
                                            withStyle(GomType.body.copy(color = Mute2).toSpanStyle()) {
                                                append(" · ${GeoUtils.radiusLabel(state.radius)}")
                                            }
                                        },
                                        style = GomType.body,
                                    )
                                }
                                inner()
                            }
                        },
                    )
                }
                CardIconButton(
                    icon = if (favorites.isEmpty()) Icons.Rounded.BookmarkBorder else Icons.Rounded.Bookmark,
                    contentDescription = "내 기록",
                    onClick = onOpenRecords,
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                HomeChip(
                    text = "전체 ${state.places.size}",
                    selected = state.categoryFilter.isEmpty(),
                    onClick = { viewModel.toggleCategoryFilter(null) },
                )
                HomeChip(
                    text = "♥ ${state.places.count { it.id in state.favoriteIds }}",
                    selected = state.favoritesOnly,
                    selectedColor = Paprika,
                    onClick = viewModel::toggleFavoritesOnly,
                )
                categoryCounts.forEach { (category, count) ->
                    HomeChip(
                        text = "$category $count",
                        selected = category in state.categoryFilter,
                        onClick = { viewModel.toggleCategoryFilter(category) },
                    )
                }
                Row(
                    modifier = Modifier
                        .height(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(PaprikaTint)
                        .clickable { showCandidateSheet = true }
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(Icons.Rounded.Block, contentDescription = "후보 조정", tint = PaprikaDeep, modifier = Modifier.size(16.dp))
                    Text(if (excludedCount == 0) "이건 빼고" else "빼고 $excludedCount", style = GomType.bodyS.copy(color = PaprikaDeep))
                }
            }

            if (mapMovedAway) {
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 10.dp)
                        .shadow(6.dp, RoundedCornerShape(20.dp))
                        .clip(RoundedCornerShape(20.dp))
                        .background(Ink)
                        .clickable {
                            val center = kakaoMap?.cameraPosition?.position
                            if (center != null) viewModel.searchAt(center.latitude, center.longitude) else viewModel.search()
                        }
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, tint = Cream, modifier = Modifier.size(16.dp))
                    Text("이 위치에서 재검색", style = GomType.bodyS.copy(color = Cream, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium))
                }
            }
        }

        if (state.loading) {
            CircularProgressIndicator(color = Paprika, modifier = Modifier.align(Alignment.Center))
        }

        // ---- 하단: 플로팅 버튼 + 카드 캐러셀 + CTA ----
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding(),
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(end = 16.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CardIconButton(Icons.Rounded.MyLocation, "내 위치", onClick = viewModel::refreshFromMyLocation)
            }

            if (state.searched && visible.isEmpty() && !state.loading) {
                EmptyCard(
                    radius = state.radius,
                    onExpand = { viewModel.setRadius(if (state.radius < 1000) 1000 else 2000) },
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            } else {
                LazyRow(
                    state = listState,
                    flingBehavior = rememberSnapFlingBehavior(listState),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    itemsIndexed(visible, key = { _, p -> p.id }) { index, place ->
                        val lastNote = visits.firstOrNull { it.placeId == place.id && it.note.isNotBlank() }?.note
                        PlaceCard(
                            index = index + 1,
                            place = place,
                            selected = index == selectedIndex,
                            isFavorite = favorites.any { it.id == place.id },
                            note = lastNote,
                            onClick = { onOpenPlace(place) },
                            onToggleFavorite = { viewModel.toggleFavorite(place) },
                        )
                    }
                }
            }

            FilledCta(
                onClick = { if (viewModel.startRoulette()) onOpenRoulette() },
                enabled = candidateCount > 0,
                height = 60.dp,
                radius = 18.dp,
                shadow = true,
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp)
                    .fillMaxWidth(),
            ) {
                Icon(Icons.Rounded.Casino, contentDescription = null, modifier = Modifier.size(26.dp))
                Text("골라줘 내 점심", style = GomType.numeral.copy(color = Cream, fontSize = 22.sp))
                Text("후보 ${candidateCount}곳", style = GomType.meta.copy(color = Cream.copy(alpha = 0.85f)))
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 230.dp),
        ) { data -> Snackbar(snackbarData = data, containerColor = Ink, contentColor = Cream) }
    }

    if (showCandidateSheet) {
        CandidateSheet(
            viewModel = viewModel,
            onStart = {
                showCandidateSheet = false
                if (viewModel.startRoulette()) onOpenRoulette()
            },
            onDismiss = { showCandidateSheet = false },
        )
    }
}

@Composable
private fun HomeChip(text: String, selected: Boolean, onClick: () -> Unit, selectedColor: Color = Ink) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = Modifier
            .height(32.dp)
            .clip(shape)
            .background(if (selected) selectedColor else Color.White)
            .border(1.5.dp, if (selected) selectedColor else Sand, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = GomType.bodyS.copy(color = if (selected) Cream else Ink, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium))
    }
}

@Composable
private fun PlaceCard(
    index: Int,
    place: Place,
    selected: Boolean,
    isFavorite: Boolean,
    note: String?,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = Modifier
            .width(CardWidth)
            .shadow(6.dp, shape, ambientColor = Ink.copy(alpha = 0.3f), spotColor = Ink.copy(alpha = 0.3f))
            .clip(shape)
            .background(Color.White)
            .border(1.5.dp, if (selected) Paprika else Sand, shape)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Paprika),
                contentAlignment = Alignment.Center,
            ) { Text("$index", style = GomType.badge.copy(color = Color.White, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)) }
            EllipsisText(place.name, GomType.titleM, modifier = Modifier.weight(1f))
            Icon(
                imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = "즐겨찾기",
                tint = if (isFavorite) Paprika else HeartOff,
                modifier = Modifier
                    .size(20.dp)
                    .clickable(onClick = onToggleFavorite),
            )
        }
        Text(place.metaLine(), style = GomType.meta)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Cream)
                .padding(horizontal = 8.dp, vertical = 6.dp),
        ) {
            EllipsisText(note ?: "아직 기록 없음", GomType.meta.copy(color = if (note == null) Mute2 else Ink))
        }
    }
}

@Composable
private fun EmptyCard(radius: Int, onExpand: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color.White)
            .border(1.5.dp, Sand, shape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("이 근처엔 아직 맛집이 없네요.\n반경을 늘려볼까요?", style = GomType.body)
        if (radius < 2000) {
            OutlinedCta(onClick = onExpand, height = 40.dp, radius = 12.dp) {
                Text("반경 ${GeoUtils.radiusLabel(if (radius < 1000) 1000 else 2000)}로")
            }
        }
    }
}

