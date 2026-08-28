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
import info.hkdevstudio.gom.util.LocationProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MapUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val keyword: String = "",
    val radius: Int = SettingsRepository.DEFAULT_RADIUS,
    val centerLat: Double = DEFAULT_LAT,
    val centerLng: Double = DEFAULT_LNG,
    val hasLocation: Boolean = false,
    val places: List<Place> = emptyList(),
    val excludedCategories: Set<String> = emptySet(),
    val selectedPlace: Place? = null,
    val rouletteCandidates: List<Place>? = null,
) {
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
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val visits: StateFlow<List<VisitEntity>> = dao.visits()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val excludedPlaces: StateFlow<List<ExcludedEntity>> = dao.excluded()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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
                    _state.update {
                        it.copy(
                            loading = false,
                            places = places,
                            error = if (places.isEmpty()) "주변에서 \"$query\" 결과를 찾지 못했어요" else null,
                        )
                    }
                }
                .onFailure { throwable ->
                    _state.update {
                        it.copy(loading = false, error = "검색 실패: ${throwable.message}")
                    }
                }
        }
    }

    /** 제외 필터를 통과한 룰렛 후보 목록. */
    fun eligiblePlaces(): List<Place> {
        val current = _state.value
        val excludedIds = excludedPlaces.value.map { it.id }.toSet()
        return current.places.filter {
            it.id !in excludedIds && it.category !in current.excludedCategories
        }
    }

    fun spin() {
        val eligible = eligiblePlaces()
        if (eligible.isEmpty()) {
            _state.update { it.copy(error = "뽑을 후보가 없어요 — 먼저 검색하거나 제외 필터를 풀어주세요") }
            return
        }
        _state.update {
            it.copy(rouletteCandidates = eligible.shuffled().take(MAX_ROULETTE_CANDIDATES))
        }
    }

    fun dismissRoulette() = _state.update { it.copy(rouletteCandidates = null) }

    fun selectPlace(place: Place?) = _state.update { it.copy(selectedPlace = place) }

    fun clearError() = _state.update { it.copy(error = null) }

    fun setRadius(value: Int) {
        viewModelScope.launch {
            settings.setRadius(value)
            search()
        }
    }

    fun toggleExcludeCategory(category: String) {
        viewModelScope.launch { settings.toggleExcludedCategory(category) }
    }

    fun isFavorite(placeId: String): Boolean = favorites.value.any { it.id == placeId }

    fun toggleFavorite(place: Place) {
        viewModelScope.launch {
            if (isFavorite(place.id)) {
                dao.removeFavorite(place.id)
            } else {
                dao.addFavorite(
                    FavoriteEntity(
                        id = place.id,
                        name = place.name,
                        category = place.category,
                        address = place.address,
                        lat = place.lat,
                        lng = place.lng,
                        placeUrl = place.placeUrl,
                    )
                )
            }
        }
    }

    fun removeFavorite(id: String) {
        viewModelScope.launch { dao.removeFavorite(id) }
    }

    fun toggleExcludePlace(place: Place) {
        viewModelScope.launch {
            if (excludedPlaces.value.any { it.id == place.id }) {
                dao.include(place.id)
            } else {
                dao.exclude(ExcludedEntity(id = place.id, name = place.name))
            }
        }
    }

    fun includePlace(id: String) {
        viewModelScope.launch { dao.include(id) }
    }

    fun recordVisit(place: Place) {
        viewModelScope.launch {
            dao.addVisit(
                VisitEntity(
                    placeId = place.id,
                    name = place.name,
                    category = place.category,
                    placeUrl = place.placeUrl,
                )
            )
        }
    }

    fun updateVisit(visitId: Long, rating: Int, note: String) {
        viewModelScope.launch { dao.updateVisit(visitId, rating, note) }
    }

    fun deleteVisit(visit: VisitEntity) {
        viewModelScope.launch { dao.deleteVisit(visit) }
    }

    companion object {
        private const val MAX_ROULETTE_CANDIDATES = 8
    }
}
