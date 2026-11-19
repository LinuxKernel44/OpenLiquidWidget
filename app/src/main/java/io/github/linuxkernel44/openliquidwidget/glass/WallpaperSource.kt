package io.github.linuxkernel44.openliquidwidget.glass

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.Shader
import android.net.Uri
import androidx.core.graphics.createBitmap
import io.github.linuxkernel44.openliquidwidget.data.AppPrefs
import io.github.linuxkernel44.openliquidwidget.data.WallpaperMode
import java.io.File

/**
 * Supplies the screen-sized bitmap that the glass refracts.
 *
 * Android 13+ only lets an app read the *system* wallpaper with "All files access", so [WallpaperMode.Auto] tries that and
 * quietly falls back to a generated mesh gradient. [WallpaperMode.Custom] uses an image the user picked.
 */
object WallpaperSource {
    private var cache: Bitmap? = null
    private var cacheKey: String? = null

    private fun customFile(ctx: Context) = File(ctx.filesDir, "wallpaper.jpg")

    /** True when the system wallpaper can actually be read (All files access granted and a readable image). */
    fun systemReadable(ctx: Context): Boolean = loadSystem(ctx) != null

    @Synchronized
    fun load(ctx: Context): Bitmap {
        val app = ctx.applicationContext
        val prefs = AppPrefs(app)
        val dm = app.resources.displayMetrics
        val key = "${prefs.wallpaperMode}:${prefs.wallpaperRevision}:${dm.widthPixels}x${dm.heightPixels}"
        cache?.let { if (key == cacheKey) return it }
        val loaded = when (prefs.wallpaperMode) {
            WallpaperMode.Auto -> loadSystem(app)
            WallpaperMode.Custom -> loadCustom(app)
            WallpaperMode.Generated -> null
        }
        if (loaded != null || prefs.wallpaperMode == WallpaperMode.Generated) {
            cache = loaded ?: generate(dm.widthPixels, dm.heightPixels)
            cacheKey = key
            return cache!!
        }
        // Fallback (e.g. no all-files access yet): not cached, so granting access takes effect on the next render.
        return generate(dm.widthPixels, dm.heightPixels)
    }

    /** Saves a picked image as the custom backdrop. */
    fun importCustom(ctx: Context, uri: Uri): Boolean = runCatching {
        val dm = ctx.resources.displayMetrics
        val src = decodeSampled(maxOf(dm.widthPixels, dm.heightPixels)) { ctx.contentResolver.openInputStream(uri) } ?: return false
        val fitted = coverTo(src, dm.widthPixels, dm.heightPixels)
        customFile(ctx).outputStream().use { fitted.compress(Bitmap.CompressFormat.JPEG, 92, it) }
        true
    }.getOrDefault(false)

    private fun loadCustom(ctx: Context): Bitmap? =
        customFile(ctx).takeIf { it.exists() }?.let { BitmapFactory.decodeFile(it.path) }

    private fun loadSystem(ctx: Context): Bitmap? = runCatching {
        val dm = ctx.resources.displayMetrics
        val d = WallpaperManager.getInstance(ctx).drawable ?: return null
        if (d.intrinsicWidth <= 0 || d.intrinsicHeight <= 0) return null
        // Draw at a bounded size, then cover-crop to the screen.
        val k = minOf(1f, maxOf(dm.widthPixels, dm.heightPixels) * 1.5f / maxOf(d.intrinsicWidth, d.intrinsicHeight))
        val w = (d.intrinsicWidth * k).toInt().coerceAtLeast(1)
        val h = (d.intrinsicHeight * k).toInt().coerceAtLeast(1)
        val raw = createBitmap(w, h)
        d.setBounds(0, 0, w, h)
        d.draw(Canvas(raw))
        coverTo(raw, dm.widthPixels, dm.heightPixels)
    }.getOrNull()

    private fun coverTo(src: Bitmap, w: Int, h: Int): Bitmap {
        val out = createBitmap(w, h)
        val k = maxOf(w / src.width.toFloat(), h / src.height.toFloat())
        val cw = w / k
        val ch = h / k
        val l = ((src.width - cw) / 2).toInt()
        val t = ((src.height - ch) / 2).toInt()
        Canvas(out).drawBitmap(src, Rect(l, t, (l + cw).toInt(), (t + ch).toInt()), Rect(0, 0, w, h), Paint(Paint.FILTER_BITMAP_FLAG))
        return out
    }

    private fun decodeSampled(maxDimension: Int, open: () -> java.io.InputStream?): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        open()?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= maxDimension && bounds.outHeight / (sample * 2) >= maxDimension) sample *= 2
        return open()?.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) }
    }

    /** A colourful mesh-style backdrop, so the glass always has something to refract. */
    fun generate(w: Int, h: Int): Bitmap {
        val bmp = createBitmap(w, h)
        val c = Canvas(bmp)
        c.drawColor(Color.parseColor("#0B1030"))
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val blobs = listOf(
            Triple(0.15f to 0.12f, 0.75f, "#FF3D7F"),
            Triple(0.90f to 0.22f, 0.70f, "#3D8BFF"),
            Triple(0.30f to 0.52f, 0.80f, "#7B3DFF"),
            Triple(0.85f to 0.62f, 0.75f, "#00D4B4"),
            Triple(0.20f to 0.92f, 0.80f, "#FFB03D"),
            Triple(0.80f to 0.95f, 0.65f, "#FF5A3D"),
        )
        val base = maxOf(w, h)
        for ((pos, rad, col) in blobs) {
            val cx = pos.first * w
            val cy = pos.second * h
            val r = rad * base * 0.55f
            val color = Color.parseColor(col)
            p.shader = RadialGradient(cx, cy, r, intArrayOf(Color.argb(210, Color.red(color), Color.green(color), Color.blue(color)), Color.argb(0, Color.red(color), Color.green(color), Color.blue(color))), null, Shader.TileMode.CLAMP)
            c.drawCircle(cx, cy, r, p)
        }
        return bmp
    }
}
