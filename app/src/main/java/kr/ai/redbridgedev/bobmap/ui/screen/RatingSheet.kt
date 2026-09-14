package kr.ai.redbridgedev.bobmap.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kr.ai.redbridgedev.bobmap.ui.MainViewModel
import kr.ai.redbridgedev.bobmap.ui.theme.Cream
import kr.ai.redbridgedev.bobmap.ui.theme.GomType
import kr.ai.redbridgedev.bobmap.ui.theme.Ink
import kr.ai.redbridgedev.bobmap.ui.theme.Mute2
import kr.ai.redbridgedev.bobmap.ui.theme.Paprika
import kr.ai.redbridgedev.bobmap.ui.theme.PaprikaDeep
import kr.ai.redbridgedev.bobmap.ui.theme.Sand
import kr.ai.redbridgedev.bobmap.ui.theme.SandDeep
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val RatingLabels = listOf("", "다신 안 가요", "그냥 그래요", "무난해요", "또 올 만해요", "여기다!")
private val QuickTags = listOf("웨이팅 있음", "양 많음", "혼밥 OK", "가성비")

/**
 * S8. 별점 시트. 결정 직후 자동 노출 + 매장 정보 CTA + 기록 행 수정에 공용.
 * 버튼: [취소] [나중에] [기록 저장]
 * @param onCancel 아무것도 남기지 않고 닫기
 * @param onLater 별점 없이 방문만 기록(신규일 때만, null이면 숨김)
 * @param onDelete null이면 삭제 버튼 없음(신규 기록)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RatingSheet(
    placeName: String,
    visitedAt: Long,
    visitNumber: Int,
    initialRating: Int,
    initialNote: String,
    onSave: (rating: Int, note: String) -> Unit,
    onCancel: () -> Unit,
    onLater: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var rating by remember { mutableIntStateOf(initialRating) }
    var note by remember { mutableStateOf(initialNote) }
    val dateText = remember(visitedAt) {
        SimpleDateFormat("M월 d일 (E) HH:mm", Locale.KOREAN).format(Date(visitedAt))
    }

    ModalBottomSheet(
        onDismissRequest = onCancel,
        sheetState = sheetState,
        containerColor = Cream,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        scrimColor = Ink.copy(alpha = 0.45f),
        dragHandle = { SheetHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding(),
        ) {
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("$dateText · ${visitNumber}번째 방문", style = GomType.bodyS.copy(color = PaprikaDeep))
                Text("$placeName,\n오늘 어땠어요?", style = GomType.displayS.copy(lineHeight = 36.sp))
            }

            // 별 5개
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            ) {
                repeat(5) { i ->
                    val filled = i < rating
                    Icon(
                        imageVector = if (filled) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                        contentDescription = "${i + 1}점",
                        tint = if (filled) Paprika else SandDeep,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { rating = i + 1 },
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .padding(top = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (rating > 0) Text(RatingLabels[rating], style = GomType.numeral.copy(color = Paprika))
            }

            // 메모
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .border(1.5.dp, Sand, RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                BasicTextField(
                    value = note,
                    onValueChange = { note = it.take(MainViewModel.MAX_NOTE_LENGTH) },
                    textStyle = GomType.body.copy(fontSize = 15.sp, lineHeight = 22.sp, color = Ink),
                    cursorBrush = SolidColor(Paprika),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 44.dp),
                    decorationBox = { inner ->
                        Box {
                            if (note.isEmpty()) {
                                Text("등심 두툼, 줄 김…", style = GomType.body.copy(fontSize = 15.sp, color = Mute2))
                            }
                            inner()
                        }
                    },
                )
                Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                    Text("나만의 한 줄 · 선택", style = GomType.meta.copy(color = Mute2), modifier = Modifier.weight(1f))
                    Text("${note.length}/${MainViewModel.MAX_NOTE_LENGTH}", style = GomType.meta.copy(color = Mute2))
                }
            }

            // 빠른 태그
            Row(
                modifier = Modifier.padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                QuickTags.forEach { tag ->
                    Box(
                        modifier = Modifier
                            .height(30.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(Color.White)
                            .border(1.5.dp, Sand, RoundedCornerShape(9.dp))
                            .clickable {
                                if (!note.contains(tag)) {
                                    val joined = if (note.isBlank()) tag else "$note, $tag"
                                    note = joined.take(MainViewModel.MAX_NOTE_LENGTH)
                                }
                            }
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text(tag, style = GomType.meta.copy(color = Ink)) }
                }
            }

            Row(
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 12.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedCta(onClick = onCancel, height = 56.dp, radius = 16.dp, background = Cream) {
                    Text("취소")
                }
                if (onLater != null) {
                    OutlinedCta(onClick = onLater, height = 56.dp, radius = 16.dp, background = Cream) {
                        Text("나중에")
                    }
                }
                FilledCta(
                    onClick = { onSave(rating, note.trim()) },
                    enabled = rating > 0,
                    height = 56.dp,
                    radius = 16.dp,
                    modifier = Modifier.weight(1f),
                ) { Text("기록 저장") }
            }
            if (onDelete != null) {
                Text(
                    "이 기록 삭제",
                    style = GomType.bodyS.copy(color = PaprikaDeep),
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onDelete)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
