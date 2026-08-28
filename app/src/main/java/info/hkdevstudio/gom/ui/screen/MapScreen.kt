package info.hkdevstudio.gom.ui.screen

import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapView
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles
import info.hkdevstudio.gom.BuildConfig
import info.hkdevstudio.gom.domain.Place
import info.hkdevstudio.gom.ui.MainViewModel
import info.hkdevstudio.gom.util.LocationProvider
import info.hkdevstudio.gom.util.MarkerBitmaps

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: MainViewModel,
    onOpenRecords: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val excludedPlaces by viewModel.excludedPlaces.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var kakaoMap by remember { mutableStateOf<KakaoMap?>(null) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var showRadiusDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.refreshFromMyLocation()
    }

    LaunchedEffect(Unit) {
        if (LocationProvider.hasPermission(context)) {
            viewModel.refreshFromMyLocation()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION,
                )
            )
        }
    }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    // 지도에 마커 반영
    LaunchedEffect(kakaoMap, state.places, state.hasLocation, state.centerLat, state.centerLng) {
        val map = kakaoMap ?: return@LaunchedEffect
        updateLabels(map, state.places, state.centerLat, state.centerLng, state.hasLocation)
    }

    // 검색 중심이 바뀌면 카메라 이동
    LaunchedEffect(kakaoMap, state.centerLat, state.centerLng) {
        kakaoMap?.moveCamera(
            CameraUpdateFactory.newCenterPosition(LatLng.from(state.centerLat, state.centerLng), 16)
        )
    }

    // 선택된 장소로 카메라 이동
    LaunchedEffect(state.selectedPlace) {
        val place = state.selectedPlace ?: return@LaunchedEffect
        kakaoMap?.moveCamera(
            CameraUpdateFactory.newCenterPosition(LatLng.from(place.lat, place.lng), 17)
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (BuildConfig.KAKAO_NATIVE_APP_KEY.isBlank()) {
            Surface(modifier = Modifier.fillMaxSize()) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "카카오 네이티브 앱 키가 없어 지도를 표시할 수 없습니다.\nlocal.properties 에 KAKAO_NATIVE_APP_KEY 를 설정하세요.",
                        modifier = Modifier.padding(32.dp),
                    )
                }
            }
        } else {
            KakaoMapView(
                onMapReady = { kakaoMap = it },
                onLabelClick = { placeId ->
                    viewModel.selectPlace(state.places.find { it.id == placeId })
                },
                modifier = Modifier.fillMaxSize(),
            )
        }

        // 상단 검색 바
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(WindowInsets.statusBars.asPaddingValues())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                tonalElevation = 3.dp,
                shadowElevation = 4.dp,
            ) {
                OutlinedTextField(
                    value = state.keyword,
                    onValueChange = viewModel::onKeywordChange,
                    placeholder = { Text("검색어 (기본: 맛집)") },
                    singleLine = true,
                    leadingIcon = {
                        IconButton(onClick = { showFilterSheet = true }) {
                            Icon(Icons.Default.FilterAlt, contentDescription = "제외 필터")
                        }
                    },
                    trailingIcon = {
                        IconButton(onClick = viewModel::search) {
                            Icon(Icons.Default.Search, contentDescription = "검색")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    tonalElevation = 3.dp,
                    shadowElevation = 4.dp,
                ) {
                    TextButton(onClick = { showRadiusDialog = true }) {
                        Text("반경 ${state.radius}m")
                    }
                }
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    tonalElevation = 3.dp,
                    shadowElevation = 4.dp,
                ) {
                    TextButton(onClick = {
                        val center = kakaoMap?.cameraPosition?.position
                        if (center != null) {
                            viewModel.searchAt(center.latitude, center.longitude)
                        } else {
                            viewModel.search()
                        }
                    }) {
                        Text("이 위치에서 재검색")
                    }
                }
            }
        }

        if (state.loading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }

        // 우측 하단 컨트롤 + 룰렛 FAB + 광고
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(WindowInsets.navigationBars.asPaddingValues()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FloatingActionButton(onClick = onOpenRecords) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = "내 기록")
                    }
                    FloatingActionButton(onClick = viewModel::refreshFromMyLocation) {
                        Icon(Icons.Default.MyLocation, contentDescription = "내 위치")
                    }
                }
                ExtendedFloatingActionButton(
                    onClick = viewModel::spin,
                    icon = { Icon(Icons.Default.Casino, contentDescription = null) },
                    text = { Text("골라줘 내 점심") },
                )
            }
            AdBanner(modifier = Modifier.fillMaxWidth())
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 120.dp),
        ) { data -> Snackbar(snackbarData = data) }
    }

    state.selectedPlace?.let { place ->
        PlaceDetailSheet(
            place = place,
            isFavorite = favorites.any { it.id == place.id },
            isExcluded = excludedPlaces.any { it.id == place.id },
            onToggleFavorite = { viewModel.toggleFavorite(place) },
            onToggleExclude = { viewModel.toggleExcludePlace(place) },
            onRecordVisit = { viewModel.recordVisit(place) },
            onDismiss = { viewModel.selectPlace(null) },
        )
    }

    state.rouletteCandidates?.let { candidates ->
        RouletteDialog(
            candidates = candidates,
            onDecide = { place ->
                viewModel.recordVisit(place)
                viewModel.dismissRoulette()
                viewModel.selectPlace(place)
            },
            onShowOnMap = { place ->
                viewModel.dismissRoulette()
                viewModel.selectPlace(place)
            },
            onDismiss = viewModel::dismissRoulette,
        )
    }

    if (showFilterSheet) {
        FilterSheet(
            categories = state.places.map { it.category }.filter { it.isNotBlank() }.distinct().sorted(),
            excludedCategories = state.excludedCategories,
            excludedPlaces = excludedPlaces,
            onToggleCategory = viewModel::toggleExcludeCategory,
            onIncludePlace = viewModel::includePlace,
            onDismiss = { showFilterSheet = false },
        )
    }

    if (showRadiusDialog) {
        RadiusDialog(
            radius = state.radius,
            onConfirm = { radius ->
                showRadiusDialog = false
                viewModel.setRadius(radius)
            },
            onDismiss = { showRadiusDialog = false },
        )
    }
}

@Composable
private fun KakaoMapView(
    onMapReady: (KakaoMap) -> Unit,
    onLabelClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var mapView by remember { mutableStateOf<MapView?>(null) }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            MapView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                mapView = this
                start(
                    object : MapLifeCycleCallback() {
                        override fun onMapDestroy() {}
                        override fun onMapError(error: Exception?) {}
                    },
                    object : KakaoMapReadyCallback() {
                        override fun onMapReady(map: KakaoMap) {
                            map.setOnLabelClickListener { _, _, label ->
                                (label.tag as? String)?.let(onLabelClick)
                                true
                            }
                            onMapReady(map)
                        }

                        override fun getPosition(): LatLng =
                            LatLng.from(37.5662952, 126.9779451)

                        override fun getZoomLevel(): Int = 16
                    },
                )
            }
        },
    )

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView?.resume()
                Lifecycle.Event.ON_PAUSE -> mapView?.pause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}

private fun updateLabels(
    map: KakaoMap,
    places: List<Place>,
    centerLat: Double,
    centerLng: Double,
    hasLocation: Boolean,
) {
    val manager = map.labelManager ?: return
    val layer = manager.layer ?: return
    layer.removeAll()

    val placeStyles = manager.addLabelStyles(
        LabelStyles.from(LabelStyle.from(MarkerBitmaps.pin(MarkerBitmaps.MINT)))
    )
    places.forEach { place ->
        layer.addLabel(
            LabelOptions.from(LatLng.from(place.lat, place.lng))
                .setStyles(placeStyles)
                .setTag(place.id)
        )
    }

    if (hasLocation) {
        val meStyles = manager.addLabelStyles(
            LabelStyles.from(LabelStyle.from(MarkerBitmaps.currentLocation()))
        )
        layer.addLabel(
            LabelOptions.from(LatLng.from(centerLat, centerLng))
                .setStyles(meStyles)
                .setTag("me")
        )
    }
}
