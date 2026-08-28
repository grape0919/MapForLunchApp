package info.hkdevstudio.gom.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path

/**
 * 마커 이미지를 리소스 대신 코드로 그린다.
 * 핀 모양: 원 + 아래 꼬리, 가운데 흰 점.
 */
object MarkerBitmaps {

    val MINT = Color.rgb(14, 155, 130)
    val ORANGE = Color.rgb(240, 130, 40)
    val BLUE = Color.rgb(50, 110, 230)

    fun pin(color: Int, sizePx: Int = 72): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val cx = sizePx / 2f
        val headRadius = sizePx * 0.30f
        val headCy = sizePx * 0.34f

        paint.color = color
        val tail = Path().apply {
            moveTo(cx - headRadius * 0.75f, headCy + headRadius * 0.55f)
            lineTo(cx, sizePx * 0.95f)
            lineTo(cx + headRadius * 0.75f, headCy + headRadius * 0.55f)
            close()
        }
        canvas.drawPath(tail, paint)
        canvas.drawCircle(cx, headCy, headRadius, paint)

        paint.color = Color.WHITE
        canvas.drawCircle(cx, headCy, headRadius * 0.38f, paint)

        return bitmap
    }

    /** 현재 위치 표시: 파란 점 + 반투명 링. */
    fun currentLocation(sizePx: Int = 56): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val cx = sizePx / 2f

        paint.color = Color.argb(60, 50, 110, 230)
        canvas.drawCircle(cx, cx, sizePx * 0.5f, paint)
        paint.color = Color.WHITE
        canvas.drawCircle(cx, cx, sizePx * 0.24f, paint)
        paint.color = BLUE
        canvas.drawCircle(cx, cx, sizePx * 0.18f, paint)

        return bitmap
    }
}
