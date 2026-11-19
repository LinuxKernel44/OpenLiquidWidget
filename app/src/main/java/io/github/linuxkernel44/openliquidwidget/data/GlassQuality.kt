package io.github.linuxkernel44.openliquidwidget.data

/** Render-resolution steps (fraction of the widget's pixel size), same ladder as OpenLiquidLauncher. */
val GLASS_RESOLUTIONS = floatArrayOf(0.25f, 0.5f, 0.75f, 1f, 1.25f, 1.5f)

/** What each quality level turns on, cumulatively. Index = [GlassQuality.level]. */
val QUALITY_LABELS = listOf("Minimum", "Low", "Current", "High", "Maximum")
val QUALITY_HINTS = listOf(
    "Blur and tint only. Lightest.",
    "Adds colour boost and rim refraction.",
    "Adds the specular edge and a soft sheen. The default look.",
    "Adds depth shading toward the edges.",
    "Adds chromatic dispersion and a softer blur. Heaviest.",
)

/**
 * App-wide glass quality. [level] 0..4 switches effects on cumulatively; [resolutionIndex] indexes [GLASS_RESOLUTIONS] and scales the
 * resolution the home-screen glass is rendered at (the app's own live glass always renders at full resolution).
 */
data class GlassQuality(val level: Int = DEFAULT_LEVEL, val resolutionIndex: Int = DEFAULT_RESOLUTION) {
    val resolution: Float get() = GLASS_RESOLUTIONS[resolutionIndex.coerceIn(0, GLASS_RESOLUTIONS.lastIndex)]

    companion object {
        const val DEFAULT_LEVEL = 2
        const val DEFAULT_RESOLUTION = 3
    }
}
