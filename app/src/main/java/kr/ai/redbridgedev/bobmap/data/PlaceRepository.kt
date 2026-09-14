package kr.ai.redbridgedev.bobmap.data

import kr.ai.redbridgedev.bobmap.data.remote.KakaoLocalApi
import kr.ai.redbridgedev.bobmap.data.remote.PlaceDocument
import kr.ai.redbridgedev.bobmap.domain.Place

class PlaceRepository(private val api: KakaoLocalApi = KakaoLocalApi.create()) {

    /**
     * 좌표 주변 반경 내 키워드 검색. 최대 [MAX_PAGES] 페이지까지 순회한다.
     */
    suspend fun search(query: String, lat: Double, lng: Double, radius: Int): Result<List<Place>> =
        runCatching {
            val places = mutableListOf<Place>()
            var page = 1
            while (page <= MAX_PAGES) {
                val response = api.searchKeyword(
                    query = query,
                    x = lng.toString(),
                    y = lat.toString(),
                    radius = radius,
                    page = page,
                )
                places += response.documents.map { it.toPlace() }
                if (response.meta.isEnd) break
                page++
            }
            places.distinctBy { it.id }
        }

    private fun PlaceDocument.toPlace(): Place {
        val depths = categoryName.split(">").map { it.trim() }
        return Place(
            id = id,
            name = placeName,
            category = depths.getOrNull(1) ?: depths.firstOrNull().orEmpty(),
            fullCategory = categoryName,
            phone = phone,
            address = roadAddressName.ifBlank { addressName },
            lat = y.toDoubleOrNull() ?: 0.0,
            lng = x.toDoubleOrNull() ?: 0.0,
            placeUrl = placeUrl,
            distanceM = distance.toIntOrNull(),
        )
    }

    companion object {
        private const val MAX_PAGES = 3
    }
}
