package io.github.linuxkernel44.openliquidwidget.data

import android.content.Context
import androidx.core.content.edit
import org.json.JSONObject

/** A city picked for the weather widget. */
data class City(val name: String, val lat: Double, val lon: Double) {
    fun toJson() = JSONObject().put("name", name).put("lat", lat).put("lon", lon).toString()

    companion object {
        fun fromJson(s: String?): City? = runCatching {
            val o = JSONObject(s ?: return null)
            City(o.getString("name"), o.getDouble("lat"), o.getDouble("lon"))
        }.getOrNull()
    }
}

/** Where the glass gets its backdrop from. */
enum class WallpaperMode { Auto, Custom, Generated }

/** App-wide settings, notes and per-widget small state (all in one SharedPreferences file). */
class AppPrefs(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("app", Context.MODE_PRIVATE)

    var wallpaperMode: WallpaperMode
        get() = runCatching { WallpaperMode.valueOf(prefs.getString("wallpaperMode", null) ?: "") }.getOrDefault(WallpaperMode.Auto)
        set(v) = prefs.edit { putString("wallpaperMode", v.name) }

    /** Bumped whenever the backdrop changes so cached bitmaps are dropped. */
    var wallpaperRevision: Long
        get() = prefs.getLong("wallpaperRevision", 0)
        set(v) = prefs.edit { putLong("wallpaperRevision", v) }

    var glassQuality: GlassQuality
        get() = GlassQuality(
            prefs.getInt("quality", GlassQuality.DEFAULT_LEVEL).coerceIn(0, QUALITY_LABELS.lastIndex),
            prefs.getInt("resolution", GlassQuality.DEFAULT_RESOLUTION).coerceIn(0, GLASS_RESOLUTIONS.lastIndex),
        )
        set(v) = prefs.edit { putInt("quality", v.level); putInt("resolution", v.resolutionIndex) }

    var fahrenheit: Boolean
        get() = prefs.getBoolean("fahrenheit", java.util.Locale.getDefault().country == "US")
        set(v) = prefs.edit { putBoolean("fahrenheit", v) }

    var note: String
        get() = prefs.getString("note", "") ?: ""
        set(v) = prefs.edit { putString("note", v) }

    /** City for widget [id]; falls back to the default city (id 0). */
    fun city(id: Int): City? = City.fromJson(prefs.getString("city.$id", null)) ?: City.fromJson(prefs.getString("city.0", null))

    fun setCity(id: Int, c: City) = prefs.edit { putString("city.$id", c.toJson()) }

    fun weatherCache(id: Int): Pair<Long, String>? {
        val s = prefs.getString("weather.$id", null) ?: return null
        return prefs.getLong("weather.$id.t", 0) to s
    }

    fun setWeatherCache(id: Int, json: String) = prefs.edit {
        putString("weather.$id", json)
        putLong("weather.$id.t", System.currentTimeMillis())
    }

    fun clearWeatherCache(id: Int) = prefs.edit { remove("weather.$id"); remove("weather.$id.t") }

    fun removeWidget(id: Int) = prefs.edit {
        remove("city.$id"); remove("weather.$id"); remove("weather.$id.t")
    }
}
