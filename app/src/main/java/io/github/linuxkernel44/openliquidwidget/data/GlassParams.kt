package io.github.linuxkernel44.openliquidwidget.data

import android.content.Context
import androidx.core.content.edit

/** Tunable look of one widget's glass. Ranges are documented per field and enforced by the configurator sliders. */
data class GlassParams(
    /** Gaussian blur of the backdrop, in dp (0..24). */
    val blur: Float = 6f,
    /** Refraction strength at the rim (0..1). */
    val refraction: Float = 0.6f,
    /** Opacity of the tint layer (0..0.6). */
    val tintAlpha: Float = 0.14f,
    /** RGB of the tint layer (alpha ignored). */
    val tintColor: Int = 0xFFFFFFFF.toInt(),
    /** Corner radius in dp (8..48). */
    val cornerDp: Float = 28f,
    /** Specular rim highlight intensity (0..1). */
    val rim: Float = 0.7f,
    /** Where, horizontally, the widget is assumed to sit on the wallpaper (0 = left edge, 1 = right edge). */
    val posX: Float = 0.5f,
    /** Where, vertically, the widget is assumed to sit on the wallpaper (0 = top, 1 = bottom). */
    val posY: Float = 0.2f,
)

/** Persists [GlassParams] per widget id, with a shared default set under id [DEFAULTS_ID]. */
class GlassStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("glass", Context.MODE_PRIVATE)

    fun get(id: Int): GlassParams = if (prefs.contains("w$id.blur")) read("w$id") else read("w$DEFAULTS_ID")

    fun save(id: Int, p: GlassParams) = prefs.edit {
        val k = "w$id"
        putFloat("$k.blur", p.blur)
        putFloat("$k.refraction", p.refraction)
        putFloat("$k.tintAlpha", p.tintAlpha)
        putInt("$k.tintColor", p.tintColor)
        putFloat("$k.cornerDp", p.cornerDp)
        putFloat("$k.rim", p.rim)
        putFloat("$k.posX", p.posX)
        putFloat("$k.posY", p.posY)
    }

    fun remove(id: Int) = prefs.edit { prefs.all.keys.filter { it.startsWith("w$id.") }.forEach(::remove) }

    private fun read(k: String): GlassParams {
        val d = GlassParams()
        return GlassParams(
            blur = prefs.getFloat("$k.blur", d.blur),
            refraction = prefs.getFloat("$k.refraction", d.refraction),
            tintAlpha = prefs.getFloat("$k.tintAlpha", d.tintAlpha),
            tintColor = prefs.getInt("$k.tintColor", d.tintColor),
            cornerDp = prefs.getFloat("$k.cornerDp", d.cornerDp),
            rim = prefs.getFloat("$k.rim", d.rim),
            posX = prefs.getFloat("$k.posX", d.posX),
            posY = prefs.getFloat("$k.posY", d.posY),
        )
    }

    companion object {
        const val DEFAULTS_ID = 0
    }
}
