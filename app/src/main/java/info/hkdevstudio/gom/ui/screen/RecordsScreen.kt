package info.hkdevstudio.gom.ui.screen

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import info.hkdevstudio.gom.data.local.FavoriteEntity
import info.hkdevstudio.gom.data.local.VisitEntity
import info.hkdevstudio.gom.ui.MainViewModel
import info.hkdevstudio.gom.ui.RouletteSource
import info.hkdevstudio.gom.ui.theme.Cream
import info.hkdevstudio.gom.ui.theme.GomType
import info.hkdevstudio.gom.ui.theme.Ink
import info.hkdevstudio.gom.ui.theme.Mute
import info.hkdevstudio.gom.ui.theme.Mute2
import info.hkdevstudio.gom.ui.theme.Paprika
import info.hkdevstudio.gom.ui.theme.PaprikaLight
import info.hkdevstudio.gom.ui.theme.Sand
import info.hkdevstudio.gom.util.GeoUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** S9. 내 기록 — 방문 기록 / 즐겨찾기. */
@Composable
fun RecordsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onOpenRoulette: () -> Unit,
    onOpenPlace: (String) -> Unit,
) {
    val favorites by viewModel.favorites.collectAsState()
    val visits by viewModel.visits.collectAsState()
    val state by viewModel.state.collectAsState()
    var tabIndex by remember { mutableIntStateOf(0) }
    var editing by remember { mutableStateOf<VisitEntity?>(null) }

    val now = Calendar.getInstance()
    val month = now.get(Calendar.MONTH) + 1
    val monthVisits = remember(visits) {
        visits.filter {
            val c = Calendar.getInstance().apply { timeInMillis = it.visitedAt }
            c.get(Calendar.YEAR) == now.get(Calendar.YEAR) && c.get(Calendar.MONTH) == now.get(Calendar.MONTH)
        }
    }
    val categorySummary = monthVisits.filter { it.category.isNotBlank() }
        .groupingBy { it.category }.eachCount().toList().sortedByDescending { it.second }.take(3)
        .joinToString(" · ") { "${it.first} ${it.second}" }
    val monthRated = monthVisits.filter { it.rating > 0 }
    val monthAvg = if (monthRated.isEmpty()) null else monthRated.map { it.rating }.average()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "뒤로", tint = Ink) }
        }

        // 헤더
        Column(modifier = Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (monthVisits.isEmpty()) {
                Text("아직 첫 점심을\n안 골랐어요", style = GomType.displayTitle)
                Text("룰렛을 돌리고 다녀오면 여기에 쌓여요", style = GomType.bodyS.copy(color = Mute))
            } else {
                Text(
                    buildAnnotatedString {
                        append("${month}월엔 ")
                        withStyle(GomType.displayTitle.copy(color = Paprika).toSpanStyle()) { append("${monthVisits.size}번") }
                        append("\n점심을 골랐어요")
                    },
                    style = GomType.displayTitle,
                )
                Text(
                    listOfNotNull(categorySummary.ifBlank { null }, monthAvg?.let { "평균 ★ ${"%.1f".format(it)}" }).joinToString(" · "),
                    style = GomType.bodyS.copy(color = Mute),
                )
            }
        }

        // 탭
        Row(
            modifier = Modifier
                .padding(start = 24.dp, end = 24.dp, top = 20.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TabLabel("방문 기록", tabIndex == 0) { tabIndex = 0 }
            TabLabel("즐겨찾기 ${favorites.size}", tabIndex == 1) { tabIndex = 1 }
        }
        HorizontalDivider(thickness = 1.5.dp, color = Sand, modifier = Modifier.padding(horizontal = 24.dp))

        if (tabIndex == 0) {
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 24.dp, vertical = 4.dp)) {
                if (visits.isEmpty()) {
                    item { Text("아직 방문 기록이 없어요", style = GomType.bodyS.copy(color = Mute2), modifier = Modifier.padding(vertical = 24.dp)) }
                }
                items(visits, key = { it.visitId }) { visit ->
                    VisitRecordRow(visit = visit, onClick = { editing = visit })
                    HorizontalDivider(thickness = 1.dp, color = Sand)
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 24.dp, vertical = 4.dp)) {
                item {
                    FilledCta(
                        onClick = { if (viewModel.startRoulette(RouletteSource.Favorites)) onOpenRoulette() },
                        enabled = favorites.isNotEmpty(),
                        background = Ink,
                        contentColor = Cream,
                        height = 48.dp,
                        radius = 14.dp,
                        modifier = Modifier
                            .padding(top = 16.dp, bottom = 8.dp)
                            .fillMaxWidth(),
                    ) {
                        Icon(Icons.Rounded.Casino, contentDescription = null, tint = PaprikaLight, modifier = Modifier.size(20.dp))
                        Text(
                            if (favorites.isEmpty()) "즐겨찾기가 아직 없어요" else "즐겨찾기 ${favorites.size}곳으로만 룰렛 돌리기",
                            style = GomType.body.copy(color = Cream, fontWeight = FontWeight.Medium),
                        )
                    }
                }
                items(favorites, key = { it.id }) { favorite ->
                    val placeVisits = visits.filter { it.placeId == favorite.id }
                    val rated = placeVisits.filter { it.rating > 0 }
                    val distance = GeoUtils.distanceMeters(state.centerLat, state.centerLng, favorite.lat, favorite.lng).toInt()
                    FavoriteRow(
                        favorite = favorite,
                        distanceM = distance,
                        visitText = if (placeVisits.isEmpty()) "아직 안 감" else buildString {
                            append("${placeVisits.size}번 감")
                            if (rated.isNotEmpty()) append(" · ★ ${"%.1f".format(rated.map { it.rating }.average())}")
                        },
                        onClick = { onOpenPlace(favorite.id) },
                        onRemove = { viewModel.removeFavorite(favorite.id) },
                    )
                    HorizontalDivider(thickness = 1.dp, color = Sand)
                }
            }
        }
    }

    editing?.let { visit ->
        val number = visits.count { it.placeId == visit.placeId && it.visitedAt <= visit.visitedAt }
        RatingSheet(
            placeName = visit.name,
            visitedAt = visit.visitedAt,
            visitNumber = number,
            initialRating = visit.rating,
            initialNote = visit.note,
            onSave = { rating, note -> viewModel.updateVisit(visit.visitId, rating, note); editing = null },
            onLater = { editing = null },
            onDelete = { viewModel.deleteVisit(visit); editing = null },
        )
    }
}

@Composable
private fun TabLabel(text: String, selected: Boolean, onClick: () -> Unit) {
    Column(modifier = Modifier.clickable(onClick = onClick)) {
        Text(
            text,
            style = if (selected) GomType.titleS else GomType.titleS.copy(color = Mute, fontWeight = FontWeight.Medium),
            modifier = Modifier.padding(start = 2.dp, end = 2.dp, bottom = 10.dp),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(if (selected) Ink else Color.Transparent),
        )
    }
}

@Composable
private fun VisitRecordRow(visit: VisitEntity, onClick: () -> Unit) {
    val day = remember(visit.visitedAt) { SimpleDateFormat("d", Locale.KOREAN).format(Date(visit.visitedAt)) }
    val weekday = remember(visit.visitedAt) { SimpleDateFormat("E", Locale.KOREAN).format(Date(visit.visitedAt)) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.width(44.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(day, style = GomType.numeral)
            Text(weekday, style = GomType.badge.copy(color = Mute))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                EllipsisText(visit.name, GomType.titleS, modifier = Modifier.weight(1f))
                if (visit.rating > 0) RatingStars(rating = visit.rating)
            }
            EllipsisText(
                text = visit.note.ifBlank { "한 줄 남기기…" },
                style = GomType.bodyS.copy(color = if (visit.note.isBlank()) Mute2 else Ink),
            )
        }
    }
}

@Composable
private fun FavoriteRow(
    favorite: FavoriteEntity,
    distanceM: Int,
    visitText: String,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .border(1.5.dp, Sand, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) { Text(favorite.name.take(1), style = GomType.numeral.copy(fontSize = GomType.titleM.fontSize)) }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            EllipsisText(favorite.name, GomType.titleS)
            Text(
                listOfNotNull(favorite.category.ifBlank { null }, "${distanceM}m", visitText).joinToString(" · "),
                style = GomType.meta,
            )
        }
        Icon(
            Icons.Rounded.Favorite,
            contentDescription = "즐겨찾기 해제",
            tint = Paprika,
            modifier = Modifier
                .size(24.dp)
                .clickable(onClick = onRemove),
        )
    }
}

