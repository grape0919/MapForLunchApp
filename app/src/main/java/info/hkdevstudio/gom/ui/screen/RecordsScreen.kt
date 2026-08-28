package info.hkdevstudio.gom.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import info.hkdevstudio.gom.data.local.VisitEntity
import info.hkdevstudio.gom.ui.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
) {
    val favorites by viewModel.favorites.collectAsState()
    val visits by viewModel.visits.collectAsState()
    var tabIndex by remember { mutableIntStateOf(0) }
    var editingVisit by remember { mutableStateOf<VisitEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("내 기록") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(padding)) {
            TabRow(selectedTabIndex = tabIndex) {
                Tab(selected = tabIndex == 0, onClick = { tabIndex = 0 }, text = { Text("즐겨찾기") })
                Tab(selected = tabIndex == 1, onClick = { tabIndex = 1 }, text = { Text("방문 기록") })
            }

            if (tabIndex == 0) {
                if (favorites.isEmpty()) {
                    EmptyHint("아직 즐겨찾기한 맛집이 없어요.\n지도에서 ♥ 를 눌러 저장해 보세요.")
                }
                LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(favorites, key = { it.id }) { favorite ->
                        Card {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(favorite.name, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = listOf(favorite.category, favorite.address)
                                            .filter { it.isNotBlank() }
                                            .joinToString(" · "),
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                                IconButton(onClick = { viewModel.removeFavorite(favorite.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "삭제")
                                }
                            }
                        }
                    }
                }
            } else {
                if (visits.isEmpty()) {
                    EmptyHint("아직 방문 기록이 없어요.\n룰렛에서 \"여기로 결정\"을 누르면 기록됩니다.")
                }
                val dateFormat = remember { SimpleDateFormat("M월 d일 (E) HH:mm", Locale.KOREAN) }
                LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(visits, key = { it.visitId }) { visit ->
                        Card(modifier = Modifier.clickable { editingVisit = visit }) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(visit.name, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = dateFormat.format(Date(visit.visitedAt)),
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RatingStars(rating = visit.rating)
                                    if (visit.note.isNotBlank()) {
                                        Text(
                                            text = "  ${visit.note}",
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    editingVisit?.let { visit ->
        VisitEditDialog(
            visit = visit,
            onSave = { rating, note ->
                viewModel.updateVisit(visit.visitId, rating, note)
                editingVisit = null
            },
            onDelete = {
                viewModel.deleteVisit(visit)
                editingVisit = null
            },
            onDismiss = { editingVisit = null },
        )
    }
}

@Composable
private fun EmptyHint(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
private fun RatingStars(
    rating: Int,
    onRate: ((Int) -> Unit)? = null,
) {
    Row {
        (1..5).forEach { star ->
            Icon(
                imageVector = if (star <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                contentDescription = "별점 $star",
                tint = if (star <= rating) Color(0xFFF2B01E) else MaterialTheme.colorScheme.outline,
                modifier = if (onRate != null) {
                    Modifier.clickable { onRate(star) }
                } else {
                    Modifier
                },
            )
        }
    }
}

@Composable
private fun VisitEditDialog(
    visit: VisitEntity,
    onSave: (Int, String) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    var rating by remember { mutableIntStateOf(visit.rating) }
    var note by remember { mutableStateOf(visit.note) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(visit.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                RatingStars(rating = rating, onRate = { rating = it })
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("나만의 한 줄 기록") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(rating, note) }) { Text("저장") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) { Text("삭제") }
                TextButton(onClick = onDismiss) { Text("취소") }
            }
        },
    )
}
