package com.matchbar.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import com.matchbar.app.R

/**
 * Construye el icono del marcador de bar a partir de [R.drawable.pin_bares].
 *
 * El PNG original es cuadrado (1024×1024) y trae margen transparente alrededor
 * del pin; si lo usáramos tal cual con anclaje inferior, el pin quedaría
 * "flotando" sobre la ubicación. Por eso recortamos el transparente y escalamos
 * conservando la proporción, de modo que el borde inferior del bitmap coincida
 * con la punta visible del pin (las pantallas anclan en ANCHOR_BOTTOM).
 */
fun barMarkerIcon(context: Context, heightDp: Int = 56): Drawable {
    val res = context.resources
    val source = BitmapFactory.decodeResource(res, R.drawable.pin_bares)
    val trimmed = trimTransparent(source)
    if (trimmed != source) source.recycle()

    val targetH = (heightDp * res.displayMetrics.density).toInt().coerceAtLeast(1)
    val targetW = (targetH * trimmed.width.toFloat() / trimmed.height).toInt().coerceAtLeast(1)
    val scaled = Bitmap.createScaledBitmap(trimmed, targetW, targetH, true)
    if (scaled != trimmed) trimmed.recycle()

    return BitmapDrawable(res, scaled)
}

/** Recorta los márgenes totalmente transparentes de un bitmap. */
private fun trimTransparent(src: Bitmap): Bitmap {
    val w = src.width
    val h = src.height
    val pixels = IntArray(w * h)
    src.getPixels(pixels, 0, w, 0, 0, w, h)

    var minX = w; var minY = h; var maxX = -1; var maxY = -1
    for (y in 0 until h) {
        val row = y * w
        for (x in 0 until w) {
            val alpha = (pixels[row + x] ushr 24) and 0xFF
            if (alpha > 20) {
                if (x < minX) minX = x
                if (x > maxX) maxX = x
                if (y < minY) minY = y
                if (y > maxY) maxY = y
            }
        }
    }
    if (maxX < minX || maxY < minY) return src // todo transparente: nada que recortar
    return Bitmap.createBitmap(src, minX, minY, maxX - minX + 1, maxY - minY + 1)
}
