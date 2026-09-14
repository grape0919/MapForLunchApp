package kr.ai.redbridgedev.bobmap.ui.screen

import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kakao.vectormap.GestureType
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapView
import com.kakao.vectormap.camera.CameraAnimation
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles
import kr.ai.redbridgedev.bobmap.domain.Place
import kr.ai.redbridgedev.bobmap.util.MarkerBitmaps

/**
 * 카카오맵 SDK v2 MapView 래퍼.
 * @param interactive false → 제스처 전부 비활성(매장 정보 미니 지도)
 */
@Composable
fun KakaoMapView(
    initialLat: Double,
    initialLng: Double,
    onMapReady: (KakaoMap) -> Unit,
    modifier: Modifier = Modifier,
    zoom: Int = 16,
    interactive: Boolean = true,
    onLabelClick: (String) -> Unit = {},
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
                            if (!interactive) {
                                GestureType.values().forEach { map.setGestureEnable(it, false) }
                            }
                            map.setOnLabelClickListener { _, _, label ->
                                (label.tag as? String)?.let(onLabelClick)
                                true
                            }
                            onMapReady(map)
                        }

                        override fun getPosition(): LatLng = LatLng.from(initialLat, initialLng)
                        override fun getZoomLevel(): Int = zoom
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
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView?.finish()
        }
    }
}

const val ME_TAG = "me"

/** 카메라 이동(기본 300ms 애니메이션). */
fun KakaoMap.animateTo(lat: Double, lng: Double, zoom: Int? = null, durationMs: Int = 300) {
    val update = if (zoom != null) CameraUpdateFactory.newCenterPosition(LatLng.from(lat, lng), zoom)
    else CameraUpdateFactory.newCenterPosition(LatLng.from(lat, lng))
    if (durationMs <= 0) moveCamera(update) else moveCamera(update, CameraAnimation.from(durationMs))
}

/**
 * 번호 핀 + 내 위치 라벨을 다시 그린다.
 * @param activeIds Paprika 배경으로 강조할 place id(선택 카드)
 * @param favoriteIds Ink 배경으로 구분할 즐겨찾기 id
 */
fun KakaoMap.renderPlaceLabels(
    places: List<Place>,
    activeIds: Set<String>,
    density: Float,
    me: Pair<Double, Double>?,
    favoriteIds: Set<String> = emptySet(),
) {
    val manager = labelManager ?: return
    val layer = manager.layer ?: return
    layer.removeAll()

    places.forEachIndexed { index, place ->
        val styles = manager.addLabelStyles(
            LabelStyles.from(
                LabelStyle.from(MarkerBitmaps.numberedPin(index + 1, place.id in activeIds, density, favorite = place.id in favoriteIds))
                    .setZoomLevel(0)
            )
        )
        layer.addLabel(
            LabelOptions.from(LatLng.from(place.lat, place.lng))
                .setStyles(styles)
                .setTag(place.id)
        )
    }

    if (me != null) {
        val meStyles = manager.addLabelStyles(
            LabelStyles.from(LabelStyle.from(MarkerBitmaps.currentLocation(density)).setZoomLevel(0))
        )
        layer.addLabel(
            LabelOptions.from(LatLng.from(me.first, me.second))
                .setStyles(meStyles)
                .setTag(ME_TAG)
        )
    }
}
