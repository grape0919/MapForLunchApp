package info.hkdevstudio.gom.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import info.hkdevstudio.gom.R
import info.hkdevstudio.gom.domain.Place
import java.io.File

/**
 * 바이럴 출구: 룰렛 결과·매장 정보를 카카오톡 등으로 내보낸다.
 * 링크는 redbridgedev.ai.kr 앱 링크 → 앱이 있으면 매장 상세, 없으면 웹 페이지가 스토어로 안내.
 */
object ShareUtils {

    const val LINK_HOST = "redbridgedev.ai.kr"
    const val PLAY_STORE_URL = "https://play.google.com/store/apps/details?id=info.hkdevstudio.gom"

    /** 매장 딥링크. 앱이 검색 결과 없이도 상세를 그릴 수 있도록 핵심 정보를 쿼리로 싣는다. */
    fun placeLink(place: Place): String =
        Uri.Builder()
            .scheme("https").authority(LINK_HOST)
            .appendPath("place").appendPath(place.id)
            .appendQueryParameter("n", place.name)
            .appendQueryParameter("c", place.category)
            .appendQueryParameter("a", place.address)
            .appendQueryParameter("lat", place.lat.toString())
            .appendQueryParameter("lng", place.lng.toString())
            .build().toString()

    /** 룰렛 결과: 티켓 카드 이미지 + 문구. */
    fun shareRouletteResult(context: Context, place: Place) {
        val text = buildString {
            appendLine("🍚 오늘 점심은 ${place.name}!")
            place.metaText()?.let { appendLine(it) }
            appendLine("김대리밥지도 룰렛이 골라줬어요")
            appendLine()
            appendLine(placeLink(place))
        }
        val image = runCatching { ticketBitmap(context, "오늘 점심은", place, "김대리밥지도 룰렛이 골라줌") }.getOrNull()
        send(context, text, image)
    }

    /** 매장 정보: 내 별점·메모가 있으면 추천처럼 붙인다. */
    fun sharePlace(context: Context, place: Place, rating: Int, note: String) {
        val mine = buildString {
            if (rating > 0) append("내 별점 ${"★".repeat(rating)}${"☆".repeat(5 - rating)}")
            if (note.isNotBlank()) { if (isNotEmpty()) append(" · "); append(note) }
        }
        val text = buildString {
            appendLine("🍚 ${place.name}")
            place.metaText()?.let { appendLine(it) }
            if (place.address.isNotBlank()) appendLine(place.address)
            if (mine.isNotBlank()) appendLine(mine)
            appendLine()
            appendLine(placeLink(place))
        }
        val image = runCatching {
            ticketBitmap(context, if (mine.isNotBlank()) "여기 어때요?" else "점심 후보", place, mine.ifBlank { "김대리밥지도" })
        }.getOrNull()
        send(context, text, image)
    }

    private fun Place.metaText(): String? = listOfNotNull(
        category.ifBlank { null },
        distanceM?.let { "${it}m · 도보 ${GeoUtils.walkMinutes(it)}분" },
    ).joinToString(" · ").ifBlank { null }

    private fun send(context: Context, text: String, image: Bitmap?) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
            if (image != null) {
                val dir = File(context.cacheDir, "share").apply { mkdirs() }
                val file = File(dir, "gom-share.png")
                file.outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                putExtra(Intent.EXTRA_STREAM, uri)
                type = "image/png"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
        context.startActivity(Intent.createChooser(intent, "공유").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    // ---- 티켓 카드 비트맵(1080×640, 1b 팔레트) ----

    private const val INK = 0xFF2A2420.toInt()
    private const val CREAM = 0xFFFBF7F0.toInt()
    private const val PAPRIKA = 0xFFD9532B.toInt()
    private const val PAPRIKA_LIGHT = 0xFFF0894F.toInt()
    private const val MUTE = 0xFF7A6F66.toInt()
    private const val SAND = 0xFFEDE4D8.toInt()

    private fun ticketBitmap(context: Context, headline: String, place: Place, footer: String): Bitmap {
        val w = 1080
        val h = 640
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val jua: Typeface = ResourcesCompat.getFont(context, R.font.jua) ?: Typeface.DEFAULT_BOLD
        val sans = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        val sansBold = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)

        // 배경(Ink) + 티켓(Cream)
        c.drawColor(INK)
        p.color = CREAM
        val card = RectF(60f, 60f, w - 60f, h - 60f)
        c.drawRoundRect(card, 44f, 44f, p)
        // 펀치 홀
        p.color = INK
        c.drawCircle(card.left, 250f, 26f, p)
        c.drawCircle(card.right, 250f, 26f, p)
        // 점선
        p.color = SAND; p.strokeWidth = 4f
        var x = card.left + 40f
        while (x < card.right - 40f) { c.drawLine(x, 250f, x + 18f, 250f, p); x += 32f }

        // 상단: 로고 타일 + 헤드라인
        p.color = PAPRIKA
        c.drawRoundRect(RectF(110f, 110f, 190f, 190f), 24f, 24f, p)
        p.color = CREAM; p.typeface = jua; p.textSize = 44f; p.textAlign = Paint.Align.CENTER
        c.drawText("밥", 150f, 150f - (p.descent() + p.ascent()) / 2f, p)
        p.textAlign = Paint.Align.LEFT
        p.color = PAPRIKA; p.typeface = jua; p.textSize = 40f
        c.drawText(headline, 220f, 132f, p)
        p.color = MUTE; p.typeface = sans; p.textSize = 26f
        c.drawText("김대리밥지도", 220f, 176f, p)

        // 가게명(Jua, 길면 축소)
        p.color = INK; p.typeface = jua
        var size = 88f
        p.textSize = size
        while (p.measureText(place.name) > card.width() - 100f && size > 44f) { size -= 4f; p.textSize = size }
        c.drawText(place.name, 110f, 370f, p)

        // 메타
        p.color = MUTE; p.typeface = sans; p.textSize = 30f
        place.metaText()?.let { c.drawText(it, 110f, 425f, p) }
        if (place.address.isNotBlank()) c.drawText(ellipsize(place.address, p, card.width() - 100f), 110f, 468f, p)

        // 하단 푸터
        p.color = PAPRIKA_LIGHT
        c.drawRoundRect(RectF(110f, 510f, 110f + p.let { it.typeface = sansBold; it.textSize = 26f; it.measureText(footer) } + 48f, 556f), 16f, 16f, p)
        p.color = CREAM
        c.drawText(footer, 134f, 542f, p)
        return bmp
    }

    private fun ellipsize(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var end = text.length
        while (end > 1 && paint.measureText(text.substring(0, end) + "…") > maxWidth) end--
        return text.substring(0, end) + "…"
    }
}
