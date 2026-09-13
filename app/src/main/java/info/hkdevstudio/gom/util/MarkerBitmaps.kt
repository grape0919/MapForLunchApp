package info.hkdevstudio.gom.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface

/**
 * 지도 마커를 리소스 대신 코드로 그린다 (1b 디자인).
 * 번호 핀: 28dp, radius 14/14/14/3, −45° 회전(꼬리가 아래), 흰 2dp 테두리, 그림자.
 */
object MarkerBitmaps {

    private const val PAPRIKA = 0xFFD9532B.toInt()
    private const val INK = 0xFF2A2420.toInt()

    /**
     * @param number 핀에 표시할 번호(1부터)
     * @param active true → Paprika 배경/흰 숫자(선택), false → 흰 배경/Ink 숫자
     * @param favorite 즐겨찾기(선택 아님) → Ink 배경/Cream 숫자로 구분
     * @param density dp → px 배율
     */
    fun numberedPin(number: Int, active: Boolean, density: Float, favorite: Boolean = false, typeface: Typeface? = null): Bitmap {
        val body = 28f * density
        val pad = 6f * density                 // 그림자 + 회전 여유
        val size = (body * 1.45f + pad * 2).toInt()
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val cx = size / 2f
        // 회전 후 꼬리 끝이 비트맵 하단에 오도록 몸통 중심을 위쪽에 둔다
        val cy = size / 2f - body * 0.18f

        val r = 14f * density
        val tail = 3f * density
        val rect = RectF(-body / 2f, -body / 2f, body / 2f, body / 2f)
        val path = Path().apply {
            addRoundRect(
                rect,
                floatArrayOf(r, r, r, r, tail, tail, r, r), // 우하단 모서리만 3dp → 회전 후 꼬리
                Path.Direction.CW,
            )
        }

        canvas.save()
        canvas.translate(cx, cy)
        canvas.rotate(45f)

        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = when {
                active -> PAPRIKA
                favorite -> INK
                else -> Color.WHITE
            }
            setShadowLayer(6f * density, 0f, 2f * density, 0x40_2A2420)
        }
        canvas.drawPath(path, fill)
        fill.clearShadowLayer()
        canvas.drawPath(path, fill)

        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f * density
            color = Color.WHITE
        }
        canvas.drawPath(path, border)
        canvas.restore()

        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (active || favorite) Color.WHITE else INK
            textSize = 12f * density
            textAlign = Paint.Align.CENTER
            this.typeface = typeface ?: Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val baseline = cy - (text.descent() + text.ascent()) / 2f
        canvas.drawText(number.toString(), cx, baseline, text)
        return bitmap
    }

    /** 내 위치: Ink 14dp 원 + 흰 3dp 테두리 + Paprika 18% 28dp 헤일로. */
    fun currentLocation(density: Float): Bitmap {
        val size = (28f * density).toInt()
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val c = size / 2f

        paint.color = Color.argb((0.18f * 255).toInt(), 0xD9, 0x53, 0x2B)
        canvas.drawCircle(c, c, c, paint)
        paint.color = Color.WHITE
        canvas.drawCircle(c, c, 10f * density, paint)
        paint.color = INK
        canvas.drawCircle(c, c, 7f * density, paint)
        return bitmap
    }
}
