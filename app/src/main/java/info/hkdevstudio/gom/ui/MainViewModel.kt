package info.hkdevstudio.gom.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import info.hkdevstudio.gom.BuildConfig
import info.hkdevstudio.gom.data.PlaceRepository
import info.hkdevstudio.gom.data.SettingsRepository
import info.hkdevstudio.gom.data.local.AppDatabase
import info.hkdevstudio.gom.data.local.ExcludedEntity
import info.hkdevstudio.gom.data.local.FavoriteEntity
import info.hkdevstudio.gom.data.local.VisitEntity
import info.hkdevstudio.gom.domain.Place
import info.hkdevstudio.gom.util.GeoUtils
import info.hkdevstudio.gom.util.LocationProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class RouletteSource { Nearby, Favorites }

data class MapUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val keyword: String = "",
    val radius: Int = SettingsRepository.DEFAULT_RADIUS,
    val centerLat: Double = DEFAULT_LAT,
    val centerLng: Double = DEFAULT_LNG,
    val hasLocation: Boolean = false,
    val searched: Boolean = false,
    val places: List<Place> = emptyList(),
    val excludedCategories: Set<String> = emptySet(),
    /** 홈 카테고리 칩(단일 선택). null = 전체 */
    val categoryFilter: String? = null,
    /** 카드 캐러셀 ↔ 지도 핀 동기화용 */
    val selectedPlaceId: String? = null,
    /** 룰렛 후보(룰렛 화면 진입 시 확정) */
    val rouletteCandidates: List<Place> = emptyList(),
    val rouletteSource: RouletteSource = RouletteSource.Nearby,
) {
    /** 카테고리 칩 필터를 적용한 목록(지도 핀·카드 번호 기준). */
    val visiblePlaces: List<Place>
        get() = if (categoryFilter == null) places else places.filter { it.category == categoryFilter }

    companion object {
        // 위치 권한 거부 시 기본 좌표: 서울시청
        const val DEFAULT_LAT = 37.5662952
        const val DEFAULT_LNG = 126.9779451
    }
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PlaceRepository()
    private val settings = SettingsRepository(application)
    private val dao = AppDatabase.get(application).gomDao()

    private val _state = MutableStateFlow(MapUiState())
    val state: StateFlow<MapUiState> = _state

    val favorites: StateFlow<List<FavoriteEntity>> = dao.favorites()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val visits: StateFlow<List<VisitEntity>> = dao.visits()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val excludedPlaces: StateFlow<List<ExcludedEntity>> = dao.excluded()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** null = 아직 로드 전(스플래시 유지), true = 첫 실행 화면 스킵 */
    val onboardingDone: StateFlow<Boolean?> = settings.onboardingDone
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        viewModelScope.launch {
            settings.radius.collect { radius -> _state.update { it.copy(radius = radius) } }
        }
        viewModelScope.launch {
            settings.excludedCategories.collect { categories ->
                _state.update { it.copy(excludedCategories = categories) }
            }
        }
    }

    // ---------- 첫 실행 ----------

    fun completeOnboarding() {
        viewModelScope.launch { settings.setOnboardingDone() }
    }

    // ---------- 검색 ----------

    fun onKeywordChange(value: String) = _state.update { it.copy(keyword = value) }

    /** 현재 위치를 갱신한 뒤 검색한다. 권한이 없으면 기본 좌표에서 검색. */
    fun refreshFromMyLocation() {
        viewModelScope.launch {
            val location = LocationProvider.currentLocation(getApplication())
            _state.update {
                it.copy(
                    centerLat = location?.first ?: MapUiState.DEFAULT_LAT,
                    centerLng = location?.second ?: MapUiState.DEFAULT_LNG,
                    hasLocation = location != null,
                )
            }
            search()
        }
    }

    /** 서울시청 기본 좌표로 시작(권한 없이 둘러보기). */
    fun startAtDefaultLocation() {
        _state.update {
            it.copy(centerLat = MapUiState.DEFAULT_LAT, centerLng = MapUiState.DEFAULT_LNG, hasLocation = false)
        }
        search()
    }

    /** 지도 카메라 중심 좌표에서 재검색. */
    fun searchAt(lat: Double, lng: Double) {
        _state.update { it.copy(centerLat = lat, centerLng = lng) }
        search()
    }

    fun search() {
        if (BuildConfig.KAKAO_REST_API_KEY.isBlank()) {
            _state.update { it.copy(error = "카카오 REST API 키가 설정되지 않았습니다 (local.properties)") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            val current = _state.value
            val query = current.keyword.ifBlank { "맛집" }
            repository.search(query, current.centerLat, current.centerLng, current.radius)
                .onSuccess { places ->
                    val sorted = places.sortedBy { it.distanceM ?: Int.MAX_VALUE }
                    _state.update {
                        it.copy(
                            loading = false,
                            searched = true,
                            places = sorted,
                            categoryFilter = it.categoryFilter?.takeIf { f -> sorted.any { p -> p.category == f } },
                            selectedPlaceId = sorted.firstOrNull()?.id,
                        )
                    }
                }
                .onFailure { throwable ->
                    _state.update {
                        it.copy(loading = false, searched = true, error = "검색 실패: ${throwable.message}")
                    }
                }
        }
    }

    fun setCategoryFilter(category: String?) {
        _state.update { s ->
            val next = s.copy(categoryFilter = category)
            next.copy(selectedPlaceId = next.visiblePlaces.firstOrNull()?.id)
        }
    }

    fun selectPlace(placeId: String?) = _state.update { it.copy(selectedPlaceId = placeId) }

    fun clearError() = _state.update { it.copy(error = null) }

    fun setRadius(value: Int) {
        if (value == _state.value.radius) return
        viewModelScope.launch {
            settings.setRadius(value)
            search()
        }
    }

    // ---------- 조회 ----------

    /** 검색 결과 → 즐겨찾기 순으로 찾는다. 즐겨찾기는 현재 중심 기준 거리를 계산. */
    fun placeById(id: String): Place? =
        _state.value.places.find { it.id == id } ?: favorites.value.find { it.id == id }?.toPlace()

    fun FavoriteEntity.toPlace(): Place {
        val s = _state.value
        return Place(
            id = id, name = name, category = category, fullCategory = category,
            phone = "", address = address, lat = lat, lng = lng, placeUrl = placeUrl,
            distanceM = GeoUtils.distanceMeters(s.centerLat, s.centerLng, lat, lng).toInt(),
        )
    }

    fun isFavorite(placeId: String): Boolean = favorites.value.any { it.id == placeId }
    fun isExcluded(placeId: String): Boolean = excludedPlaces.value.any { it.id == placeId }

    // ---------- 룰렛 ----------

    /** 제외 필터를 통과한 주변 후보(가까운 순). */
    fun eligiblePlaces(): List<Place> {
        val current = _state.value
        val excludedIds = excludedPlaces.value.map { it.id }.toSet()
        return current.places.filter { it.id !in excludedIds && it.category !in current.excludedCategories }
    }

    /** 후보 확정. 8곳 초과면 가까운 8곳(즐겨찾기 룰렛은 전체). 성공 시 true. */
    fun startRoulette(source: RouletteSource = RouletteSource.Nearby): Boolean {
        val candidates = when (source) {
            RouletteSource.Nearby -> eligiblePlaces().take(MAX_ROULETTE_CANDIDATES)
            RouletteSource.Favorites -> favorites.value.map { it.toPlace() }
        }
        if (candidates.isEmpty()) {
            _state.update { it.copy(error = "뽑을 후보가 없어요 — 제외 필터를 풀거나 반경을 늘려보세요") }
            return false
        }
        _state.update { it.copy(rouletteCandidates = candidates, rouletteSource = source) }
        return true
    }

    /** 후보 조정 시트에서 바뀐 필터를 룰렛 후보에 다시 반영. */
    fun refreshRouletteCandidates() {
        if (_state.value.rouletteSource == RouletteSource.Nearby) {
            _state.update { it.copy(rouletteCandidates = eligiblePlaces().take(MAX_ROULETTE_CANDIDATES)) }
        }
    }

    // ---------- 제외 ----------

    fun toggleExcludeCategory(category: String) {
        viewModelScope.launch { settings.toggleExcludedCategory(category) }
    }

    fun resetExclusions() {
        viewModelScope.launch {
            settings.clearExcludedCategories()
            dao.clearExcluded()
        }
    }

    fun toggleExcludePlace(place: Place) {
        viewModelScope.launch {
            if (isExcluded(place.id)) dao.include(place.id)
            else dao.exclude(ExcludedEntity(id = place.id, name = place.name))
        }
    }

    fun includePlace(id: String) {
        viewModelScope.launch { dao.include(id) }
    }

    // ---------- 즐겨찾기 ----------

    fun toggleFavorite(place: Place) {
        viewModelScope.launch {
            if (isFavorite(place.id)) {
                dao.removeFavorite(place.id)
            } else {
                dao.addFavorite(
                    FavoriteEntity(
                        id = place.id, name = place.name, category = place.category,
                        address = place.address, lat = place.lat, lng = place.lng, placeUrl = place.placeUrl,
                    )
                )
            }
        }
    }

    fun removeFavorite(id: String) {
        viewModelScope.launch { dao.removeFavorite(id) }
    }

    // ---------- 방문 기록 ----------

    /** 방문 기록 생성(별점 미입력). 생성된 visitId 반환. */
    suspend fun recordVisit(place: Place): Long =
        dao.addVisit(
            VisitEntity(placeId = place.id, name = place.name, category = place.category, placeUrl = place.placeUrl)
        )

    fun updateVisit(visitId: Long, rating: Int, note: String) {
        viewModelScope.launch { dao.updateVisit(visitId, rating.coerceIn(0, 5), note.take(MAX_NOTE_LENGTH)) }
    }

    fun deleteVisit(visit: VisitEntity) {
        viewModelScope.launch { dao.deleteVisit(visit) }
    }

    fun visitsOf(placeId: String): List<VisitEntity> = visits.value.filter { it.placeId == placeId }

    companion object {
        const val MAX_ROULETTE_CANDIDATES = 8
        const val MAX_NOTE_LENGTH = 40
    }
}
