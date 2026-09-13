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
    val excludedPlaceIds: Set<String> = emptySet(),
    val favoriteIds: Set<String> = emptySet(),
    /** 홈 카테고리 칩(복수 선택). 비어 있으면 전체 */
    val categoryFilter: Set<String> = emptySet(),
    /** 홈 ♥ 칩: 즐겨찾기만 보기 */
    val favoritesOnly: Boolean = false,
    /** 카드 캐러셀 ↔ 지도 핀 동기화용 */
    val selectedPlaceId: String? = null,
    /** 룰렛 전체 후보(필터 통과한 모든 곳) */
    val roulettePool: List<Place> = emptyList(),
    /** 이번 판 돌림판에 올라간 곳(≤ 8, pool에서 무작위) */
    val rouletteWheel: List<Place> = emptyList(),
    val rouletteSource: RouletteSource = RouletteSource.Nearby,
) {
    /** 카테고리·즐겨찾기 칩 필터를 적용한 목록(지도 핀·카드 번호 기준). */
    val visiblePlaces: List<Place>
        get() = places.filter { p ->
            (categoryFilter.isEmpty() || p.category in categoryFilter) &&
                (!favoritesOnly || p.id in favoriteIds)
        }

    /** 룰렛 후보 = 보이는 목록 − 제외 카테고리 − 제외 가게 */
    val eligiblePlaces: List<Place>
        get() = visiblePlaces.filter { it.id !in excludedPlaceIds && it.category !in excludedCategories }

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
        viewModelScope.launch {
            excludedPlaces.collect { list -> _state.update { it.copy(excludedPlaceIds = list.map { e -> e.id }.toSet()) } }
        }
        viewModelScope.launch {
            favorites.collect { list -> _state.update { it.copy(favoriteIds = list.map { f -> f.id }.toSet()) } }
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
                    _state.update { s ->
                        val next = s.copy(
                            loading = false,
                            searched = true,
                            places = sorted,
                            categoryFilter = s.categoryFilter.filter { f -> sorted.any { p -> p.category == f } }.toSet(),
                        )
                        next.copy(selectedPlaceId = next.visiblePlaces.firstOrNull()?.id)
                    }
                }
                .onFailure { throwable ->
                    _state.update {
                        it.copy(loading = false, searched = true, error = "검색 실패: ${throwable.message}")
                    }
                }
        }
    }

    /** 카테고리 칩 토글(복수 선택). null = 전체(모두 해제). */
    fun toggleCategoryFilter(category: String?) {
        _state.update { s ->
            val next = when {
                category == null -> s.copy(categoryFilter = emptySet())
                category in s.categoryFilter -> s.copy(categoryFilter = s.categoryFilter - category)
                else -> s.copy(categoryFilter = s.categoryFilter + category)
            }
            next.copy(selectedPlaceId = next.visiblePlaces.firstOrNull()?.id)
        }
    }

    fun toggleFavoritesOnly() {
        _state.update { s ->
            val next = s.copy(favoritesOnly = !s.favoritesOnly)
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

    /** 검색 결과 → 즐겨찾기 → 방문 기록 순으로 찾는다. */
    fun placeById(id: String): Place? =
        _state.value.places.find { it.id == id }
            ?: favorites.value.find { it.id == id }?.toPlace()
            ?: visits.value.find { it.placeId == id }?.toPlace()

    private fun distanceFromCenter(lat: Double, lng: Double): Int? {
        if (lat == 0.0 && lng == 0.0) return null
        val s = _state.value
        return GeoUtils.distanceMeters(s.centerLat, s.centerLng, lat, lng).toInt()
    }

    fun FavoriteEntity.toPlace(): Place = Place(
        id = id, name = name, category = category, fullCategory = category,
        phone = "", address = address, lat = lat, lng = lng, placeUrl = placeUrl,
        distanceM = distanceFromCenter(lat, lng),
    )

    private fun VisitEntity.toPlace(): Place = Place(
        id = placeId, name = name, category = category, fullCategory = category,
        phone = "", address = address, lat = lat, lng = lng, placeUrl = placeUrl,
        distanceM = distanceFromCenter(lat, lng),
    )

    fun isFavorite(placeId: String): Boolean = favorites.value.any { it.id == placeId }
    fun isExcluded(placeId: String): Boolean = excludedPlaces.value.any { it.id == placeId }

    // ---------- 룰렛 ----------

    /** 전체 후보를 확정하고 돌림판에 올릴 8곳을 무작위로 뽑는다. 성공 시 true. */
    fun startRoulette(source: RouletteSource = RouletteSource.Nearby): Boolean {
        val pool = when (source) {
            RouletteSource.Nearby -> _state.value.eligiblePlaces
            RouletteSource.Favorites -> favorites.value.map { it.toPlace() }.filter { it.id !in _state.value.excludedPlaceIds }
        }
        if (pool.isEmpty()) {
            _state.update { it.copy(error = "뽑을 후보가 없어요 — 제외 필터를 풀거나 반경을 늘려보세요") }
            return false
        }
        _state.update {
            it.copy(roulettePool = pool, rouletteWheel = pool.shuffled().take(WHEEL_SIZE), rouletteSource = source)
        }
        return true
    }

    /** "다시": 같은 후보군에서 돌림판 8곳을 다시 뽑는다. */
    fun reshuffleWheel() {
        _state.update { it.copy(rouletteWheel = it.roulettePool.shuffled().take(WHEEL_SIZE)) }
    }

    /** 후보 조정(제외/반경)이 바뀐 뒤 후보군과 돌림판을 다시 계산. */
    fun refreshRouletteCandidates() {
        _state.update { s ->
            val pool = when (s.rouletteSource) {
                RouletteSource.Nearby -> s.eligiblePlaces
                RouletteSource.Favorites -> favorites.value.map { it.toPlace() }.filter { it.id !in s.excludedPlaceIds }
            }
            // 이미 돌림판에 있던 곳은 유지하고 빠진 곳만 채운다
            val kept = s.rouletteWheel.filter { w -> pool.any { it.id == w.id } }
            val fill = pool.filter { p -> kept.none { it.id == p.id } }.shuffled().take(WHEEL_SIZE - kept.size)
            s.copy(roulettePool = pool, rouletteWheel = kept + fill)
        }
    }

    // ---------- 제외 ----------

    fun toggleExcludeCategory(category: String) {
        viewModelScope.launch {
            settings.toggleExcludedCategory(category)
            refreshRouletteCandidates()
        }
    }

    fun resetExclusions() {
        viewModelScope.launch {
            settings.clearExcludedCategories()
            dao.clearExcluded()
            refreshRouletteCandidates()
        }
    }

    fun toggleExcludePlace(place: Place) {
        viewModelScope.launch {
            if (isExcluded(place.id)) dao.include(place.id)
            else dao.exclude(ExcludedEntity(id = place.id, name = place.name))
            refreshRouletteCandidates()
        }
    }

    fun includePlace(id: String) {
        viewModelScope.launch {
            dao.include(id)
            refreshRouletteCandidates()
        }
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

    /** 방문 기록 생성. 생성된 visitId 반환. */
    suspend fun recordVisit(place: Place, rating: Int = 0, note: String = ""): Long =
        dao.addVisit(
            VisitEntity(
                placeId = place.id, name = place.name, category = place.category, placeUrl = place.placeUrl,
                rating = rating.coerceIn(0, 5), note = note.take(MAX_NOTE_LENGTH),
                lat = place.lat, lng = place.lng, address = place.address,
            )
        )

    fun updateVisit(visitId: Long, rating: Int, note: String) {
        viewModelScope.launch { dao.updateVisit(visitId, rating.coerceIn(0, 5), note.take(MAX_NOTE_LENGTH)) }
    }

    fun deleteVisit(visit: VisitEntity) {
        viewModelScope.launch { dao.deleteVisit(visit) }
    }

    companion object {
        const val WHEEL_SIZE = 8
        const val MAX_NOTE_LENGTH = 40
    }
}
