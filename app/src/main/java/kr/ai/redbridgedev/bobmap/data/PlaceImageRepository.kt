package kr.ai.redbridgedev.bobmap.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * 매장 대표 사진. 카카오 로컬 API는 이미지를 주지 않으므로
 * 플레이스 페이지(place_url)의 og:image 메타태그에서 대표 사진 URL을 읽는다.
 * 결과(없음 포함)는 프로세스 생존 동안 메모리에 캐시.
 */
object PlaceImageRepository {

    private val cache = ConcurrentHashMap<String, String>()   // placeId → url ("" = 없음)
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    private val ogImageRegex = Regex("""<meta\s+property=["']og:image["']\s+content=["']([^"']+)["']""")
    private const val MAX_BYTES = 64 * 1024L

    suspend fun thumbnail(placeId: String, placeUrl: String): String? {
        cache[placeId]?.let { return it.ifBlank { null } }
        if (placeUrl.isBlank()) return null
        val url = withContext(Dispatchers.IO) {
            runCatching {
                val request = Request.Builder()
                    .url(placeUrl.replaceFirst("http://", "https://"))
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/120 Mobile Safari/537.36")
                    .build()
                client.newCall(request).execute().use { response ->
                    val source = response.body?.source() ?: return@use null
                    // 헤더 근처에만 있으므로 앞부분만 읽는다
                    source.request(MAX_BYTES)
                    val head = source.buffer.readUtf8(minOf(source.buffer.size, MAX_BYTES))
                    ogImageRegex.find(head)?.groupValues?.get(1)
                        ?.let { if (it.startsWith("//")) "https:$it" else it }
                        // 사진이 없는 가게는 정적 지도 이미지가 오므로 제외
                        ?.takeIf { it.contains("kakaomapPhoto") || it.contains("cthumb") }
                }
            }.getOrNull()
        }
        cache[placeId] = url.orEmpty()
        return url
    }
}
