package info.hkdevstudio.gom.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import info.hkdevstudio.gom.data.SettingsRepository
import info.hkdevstudio.gom.ui.MainViewModel
import info.hkdevstudio.gom.ui.theme.Cream
import info.hkdevstudio.gom.ui.theme.GomType
import info.hkdevstudio.gom.ui.theme.Ink
import info.hkdevstudio.gom.ui.theme.Mute
import info.hkdevstudio.gom.ui.theme.Mute2
import info.hkdevstudio.gom.ui.theme.Paprika
import info.hkdevstudio.gom.ui.theme.PaprikaDeep
import info.hkdevstudio.gom.ui.theme.PaprikaLight
import info.hkdevstudio.gom.ui.theme.PaprikaTint
import info.hkdevstudio.gom.ui.theme.PaprikaTintBorder
import info.hkdevstudio.gom.ui.theme.Sand
import info.hkdevstudio.gom.ui.theme.SandDeep
import info.hkdevstudio.gom.util.GeoUtils

/** S6. 후보 조정 시트 — 제외 카테고리·제외 가게·검색 반경을 한 곳에서. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CandidateSheet(
    viewModel: MainViewModel,
    onStart: () -> Unit,
    onDismiss: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val excludedPlaces by viewModel.excludedPlaces.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val categoryCounts = remember(state.places) {
        state.places.filter { it.category.isNotBlank() }
            .groupingBy { it.category }.eachCount()
            .toList().sortedBy { it.first }
    }
    val candidateCount = state.eligiblePlaces.size

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Cream,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        scrimColor = Color.Black.copy(alpha = 0.45f),
        dragHandle = { SheetHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("오늘은 이건 빼고", style = GomType.sheetTitle, modifier = Modifier.weight(1f))
                Text(
                    "초기화",
                    style = GomType.bodyS.copy(color = PaprikaDeep),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { viewModel.resetExclusions() }
                        .padding(6.dp),
                )
            }

            SectionLabel("종류")
            FlowRow(
                modifier = Modifier.padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                categoryCounts.forEach { (category, count) ->
                    val excluded = category in state.excludedCategories
                    CategoryChip(
                        name = category,
                        count = count,
                        excluded = excluded,
                        onClick = { viewModel.toggleExcludeCategory(category) },
                    )
                }
                if (categoryCounts.isEmpty()) {
                    Text("먼저 주변을 검색해 주세요", style = GomType.bodyS.copy(color = Mute2))
                }
            }

            if (excludedPlaces.isNotEmpty()) {
                SectionLabel("당분간 빼둔 가게 · 눌러서 해제")
                FlowRow(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    excludedPlaces.forEach { entity ->
                        Row(
                            modifier = Modifier
                                .height(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .border(1.5.dp, Sand, RoundedCornerShape(12.dp))
                                .clickable { viewModel.includePlace(entity.id) }
                                .padding(start = 12.dp, end = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(entity.name, style = GomType.bodyS)
                            Icon(Icons.Rounded.Close, contentDescription = "해제", tint = Mute2, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // 검색 반경
            Row(
                modifier = Modifier
                    .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 4.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("검색 반경", style = GomType.titleS.copy(color = Mute, fontSize = GomType.bodyS.fontSize), modifier = Modifier.weight(1f))
                Text(GeoUtils.radiusLabel(state.radius), style = GomType.numeral)
                Text(" · 도보 ${state.radius / 70}분", style = GomType.meta)
            }
            val steps = SettingsRepository.RADIUS_STEPS
            var sliderIndex by remember(state.radius) { mutableFloatStateOf(steps.indexOf(state.radius).coerceAtLeast(0).toFloat()) }
            Slider(
                value = sliderIndex,
                onValueChange = { sliderIndex = it },
                onValueChangeFinished = { viewModel.setRadius(steps[sliderIndex.toInt().coerceIn(0, steps.lastIndex)]) },
                valueRange = 0f..steps.lastIndex.toFloat(),
                steps = steps.size - 2,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Paprika,
                    inactiveTrackColor = Sand,
                    activeTickColor = Color.Transparent,
                    inactiveTickColor = Color.Transparent,
                ),
                thumb = {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(3.dp, Paprika, CircleShape),
                    )
                },
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                steps.forEach { Text(GeoUtils.radiusLabel(it), style = GomType.badge.copy(color = Mute2)) }
            }

            // CTA
            val enabled = candidateCount > 0
            FilledCta(
                onClick = onStart,
                enabled = enabled,
                background = Ink,
                contentColor = Cream,
                height = 56.dp,
                radius = 16.dp,
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 12.dp)
                    .fillMaxWidth(),
            ) {
                if (enabled) {
                    Text(
                        buildAnnotatedString {
                            append("후보 ")
                            withStyle(GomType.numeral.copy(color = PaprikaLight).toSpanStyle()) { append("${candidateCount}곳") }
                            append("으로 돌리기")
                        }
                    )
                } else {
                    Text("빼고 나니 남는 게 없어요")
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = GomType.titleS.copy(color = Mute, fontSize = GomType.bodyS.fontSize),
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 10.dp),
    )
}

@Composable
private fun CategoryChip(name: String, count: Int, excluded: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .height(38.dp)
            .clip(shape)
            .background(if (excluded) PaprikaTint else Color.White)
            .border(1.5.dp, if (excluded) PaprikaTintBorder else Sand, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (excluded) {
            Icon(Icons.Rounded.Block, contentDescription = null, tint = PaprikaDeep, modifier = Modifier.size(16.dp))
        }
        Text(
            buildAnnotatedString {
                withStyle(
                    GomType.bodyS.copy(
                        color = if (excluded) PaprikaDeep else Ink,
                        textDecoration = if (excluded) TextDecoration.LineThrough else null,
                    ).toSpanStyle()
                ) { append(name) }
                withStyle(GomType.bodyS.copy(color = (if (excluded) PaprikaDeep else Ink).copy(alpha = 0.55f)).toSpanStyle()) {
                    append(" $count")
                }
            },
            style = GomType.bodyS,
        )
    }
}

