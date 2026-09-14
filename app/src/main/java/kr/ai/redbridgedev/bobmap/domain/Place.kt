package kr.ai.redbridgedev.bobmap.domain

data class Place(
    val id: String,
    val name: String,
    val category: String,       // "한식", "일식" 등 2차 분류
    val fullCategory: String,   // "음식점 > 한식 > 국밥"
    val phone: String,
    val address: String,
    val lat: Double,
    val lng: Double,
    val placeUrl: String,
    val distanceM: Int?,
)
