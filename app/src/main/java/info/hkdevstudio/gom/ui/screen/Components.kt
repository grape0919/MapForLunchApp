package info.hkdevstudio.gom.ui.screen

import android.content.Intent
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import info.hkdevstudio.gom.BuildConfig
import info.hkdevstudio.gom.data.local.ExcludedEntity
import info.hkdevstudio.gom.domain.Place

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceDetailSheet(
    place: Place,
    isFavorite: Boolean,
    isExcluded: Boolean,
    onToggleFavorite: () -> Unit,
    onToggleExclude: () -> Unit,
    onRecordVisit: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = place.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = listOfNotNull(
                            place.category.ifBlank { null },
                            place.distanceM?.let { "${it}m" },
                            place.address.ifBlank { null },
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "즐겨찾기",
                        tint = if (isFavorite) Color(0xFFE0533D) else MaterialTheme.colorScheme.onSurface,
                    )
                }
                IconButton(onClick = onToggleExclude) {
                    Icon(
                        imageVector = Icons.Default.Block,
                        contentDescription = "룰렛에서 제외",
                        tint = if (isExcluded) Color(0xFFB3402F) else MaterialTheme.colorScheme.onSurface,
                    )
                }
                IconButton(onClick = onRecordVisit) {
                    Icon(Icons.Default.RestaurantMenu, contentDescription = "방문 기록")
                }
            }

            if (place.placeUrl.isNotBlank()) {
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(420.dp),
                    factory = { context ->
                        WebView(context).apply {
                            settings.javaScriptEnabled = true
                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                ): Boolean {
                                    val url = request?.url ?: return false
                                    return when (url.scheme) {
                                        "http", "https" -> false
                                        else -> {
                                            runCatching {
                                                context.startActivity(Intent(Intent.ACTION_VIEW, url))
                                            }
                                            true
                                        }
                                    }
                                }
                            }
                            loadUrl(place.placeUrl)
                        }
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterSheet(
    categories: List<String>,
    excludedCategories: Set<String>,
    excludedPlaces: List<ExcludedEntity>,
    onToggleCategory: (String) -> Unit,
    onIncludePlace: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "오늘은 이건 빼고 🙅",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            if (categories.isEmpty()) {
                Text("먼저 주변 맛집을 검색하면 카테고리가 표시됩니다.")
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    categories.forEach { category ->
                        FilterChip(
                            selected = category in excludedCategories,
                            onClick = { onToggleCategory(category) },
                            label = { Text(category) },
                        )
                    }
                }
            }

            if (excludedPlaces.isNotEmpty()) {
                Text(
                    text = "제외한 가게 (누르면 해제)",
                    style = MaterialTheme.typography.titleSmall,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    excludedPlaces.forEach { entity ->
                        InputChip(
                            selected = true,
                            onClick = { onIncludePlace(entity.id) },
                            label = { Text(entity.name) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RadiusDialog(
    radius: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var value by remember { mutableFloatStateOf(radius.toFloat()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("맛집 검색 반경") },
        text = {
            Column {
                Text("${value.toInt()}m")
                Slider(
                    value = value,
                    onValueChange = { value = it },
                    valueRange = 100f..1000f,
                    steps = 8,
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(value.toInt()) }) { Text("적용") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        },
    )
}

@Composable
fun AdBanner(modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = BuildConfig.ADMOB_BANNER_ID
                loadAd(AdRequest.Builder().build())
            }
        },
    )
}
