package io.github.linuxkernel44.openliquidwidget.data

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class Weather(val temp: Int, val feelsLike: Int, val code: Int, val isDay: Boolean, val high: Int, val low: Int, val wind: Int) {
    val emoji: String get() = when (code) {
        0 -> if (isDay) "☀️" else "🌙"
        1, 2 -> if (isDay) "🌤️" else "☁️"
        3 -> "☁️"
        45, 48 -> "🌫️"
        in 51..57 -> "🌦️"
        in 61..67, in 80..82 -> "🌧️"
        in 71..77, 85, 86 -> "❄️"
        95, 96, 99 -> "⛈️"
        else -> "🌡️"
    }
    val description: String get() = when (code) {
        0 -> "Clear"
        1 -> "Mostly clear"
        2 -> "Partly cloudy"
        3 -> "Overcast"
        45, 48 -> "Fog"
        in 51..57 -> "Drizzle"
        in 61..65 -> "Rain"
        66, 67 -> "Freezing rain"
        in 71..77 -> "Snow"
        in 80..82 -> "Showers"
        85, 86 -> "Snow showers"
        95 -> "Thunderstorm"
        96, 99 -> "Hail storm"
        else -> "—"
    }

    fun toJson(): String = JSONObject().put("t", temp).put("f", feelsLike).put("c", code).put("d", isDay).put("h", high).put("l", low).put("w", wind).toString()

    companion object {
        fun fromJson(s: String): Weather? = runCatching {
            val o = JSONObject(s)
            Weather(o.getInt("t"), o.getInt("f"), o.getInt("c"), o.getBoolean("d"), o.getInt("h"), o.getInt("l"), o.getInt("w"))
        }.getOrNull()
    }
}

/** Tiny client for the keyless Open-Meteo forecast and geocoding APIs. Blocking; call off the main thread. */
object WeatherApi {
    fun forecast(city: City, fahrenheit: Boolean): Weather? = runCatching {
        val unit = if (fahrenheit) "fahrenheit" else "celsius"
        val windUnit = if (fahrenheit) "mph" else "kmh"
        val url = "https://api.open-meteo.com/v1/forecast?latitude=${city.lat}&longitude=${city.lon}" +
            "&current=temperature_2m,apparent_temperature,weather_code,wind_speed_10m,is_day" +
            "&daily=temperature_2m_max,temperature_2m_min&forecast_days=1&timezone=auto" +
            "&temperature_unit=$unit&wind_speed_unit=$windUnit"
        val o = JSONObject(get(url))
        val cur = o.getJSONObject("current")
        val daily = o.getJSONObject("daily")
        Weather(
            temp = Math.round(cur.getDouble("temperature_2m")).toInt(),
            feelsLike = Math.round(cur.getDouble("apparent_temperature")).toInt(),
            code = cur.getInt("weather_code"),
            isDay = cur.getInt("is_day") == 1,
            high = Math.round(daily.getJSONArray("temperature_2m_max").getDouble(0)).toInt(),
            low = Math.round(daily.getJSONArray("temperature_2m_min").getDouble(0)).toInt(),
            wind = Math.round(cur.getDouble("wind_speed_10m")).toInt(),
        )
    }.getOrNull()

    fun search(query: String): List<City> = runCatching {
        if (query.isBlank()) return emptyList()
        val q = URLEncoder.encode(query.trim(), "UTF-8")
        val arr = JSONObject(get("https://geocoding-api.open-meteo.com/v1/search?name=$q&count=6&language=en&format=json")).optJSONArray("results")
            ?: return emptyList()
        (0 until arr.length()).map {
            val r = arr.getJSONObject(it)
            val extra = listOf(r.optString("admin1"), r.optString("country")).filter { s -> s.isNotBlank() }.joinToString(", ")
            City(if (extra.isEmpty()) r.getString("name") else "${r.getString("name")}, $extra", r.getDouble("latitude"), r.getDouble("longitude"))
        }
    }.getOrDefault(emptyList())

    private fun get(url: String): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 8000
        c.readTimeout = 8000
        c.setRequestProperty("User-Agent", "OpenLiquidWidget")
        return try { c.inputStream.bufferedReader().use { it.readText() } } finally { c.disconnect() }
    }
}
