package info.hkdevstudio.gom.data.remote

import info.hkdevstudio.gom.BuildConfig
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

@Serializable
data class KeywordSearchResponse(
    val meta: Meta,
    val documents: List<PlaceDocument> = emptyList(),
)

@Serializable
data class Meta(
    @SerialName("is_end") val isEnd: Boolean = true,
    @SerialName("total_count") val totalCount: Int = 0,
    @SerialName("pageable_count") val pageableCount: Int = 0,
)

@Serializable
data class PlaceDocument(
    val id: String = "",
    @SerialName("place_name") val placeName: String = "",
    @SerialName("category_name") val categoryName: String = "",
    val phone: String = "",
    @SerialName("address_name") val addressName: String = "",
    @SerialName("road_address_name") val roadAddressName: String = "",
    val x: String = "0",
    val y: String = "0",
    @SerialName("place_url") val placeUrl: String = "",
    val distance: String = "",
)

interface KakaoLocalApi {

    @GET("v2/local/search/keyword.json")
    suspend fun searchKeyword(
        @Query("query") query: String,
        @Query("x") x: String,
        @Query("y") y: String,
        @Query("radius") radius: Int,
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 15,
        @Query("category_group_code") categoryGroupCode: String = "FD6",
    ): KeywordSearchResponse

    companion object {
        fun create(): KakaoLocalApi {
            val client = OkHttpClient.Builder()
                .addInterceptor { chain ->
                    chain.proceed(
                        chain.request().newBuilder()
                            .header("Authorization", "KakaoAK ${BuildConfig.KAKAO_REST_API_KEY}")
                            .build()
                    )
                }
                .build()
            val json = Json { ignoreUnknownKeys = true }
            return Retrofit.Builder()
                .baseUrl("https://dapi.kakao.com/")
                .client(client)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(KakaoLocalApi::class.java)
        }
    }
}
