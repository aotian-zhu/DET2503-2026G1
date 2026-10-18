package no.dte2503.volunteer.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import kotlin.math.roundToInt

internal fun mapMarker(context: Context, label: String, color: Int, selected: Boolean, facility: Boolean): BitmapDrawable {
    val density = context.resources.displayMetrics.density
    val size = ((if (selected) 38 else 30) * density).roundToInt()
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val border = 2.5f * density
    val bounds = RectF(border, border, size - border, size - border)
    paint.color = if (selected) 0xFF8CE2F1.toInt() else Color.WHITE
    if (facility) canvas.drawRoundRect(bounds, 7 * density, 7 * density, paint) else canvas.drawOval(bounds, paint)
    bounds.inset(border, border)
    paint.color = color
    if (facility) canvas.drawRoundRect(bounds, 5 * density, 5 * density, paint) else canvas.drawOval(bounds, paint)
    paint.color = Color.WHITE
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    paint.textAlign = Paint.Align.CENTER
    paint.textSize = 14 * density
    canvas.drawText(label, size / 2f, size / 2f - (paint.ascent() + paint.descent()) / 2, paint)
    return BitmapDrawable(context.resources, bitmap)
}
