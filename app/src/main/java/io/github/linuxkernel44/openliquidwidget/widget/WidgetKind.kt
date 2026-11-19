package io.github.linuxkernel44.openliquidwidget.widget

import android.appwidget.AppWidgetProvider
import androidx.annotation.StringRes
import io.github.linuxkernel44.openliquidwidget.R

/** Every widget the app ships; drives the gallery, the configurator and the bulk refresh. */
enum class WidgetKind(
    val provider: Class<out AppWidgetProvider>,
    @StringRes val title: Int,
    @StringRes val description: Int,
    /** Default size in dp, used for previews and as a fallback when the launcher reports none. */
    val defaultWidthDp: Int,
    val defaultHeightDp: Int,
) {
    Clock(ClockWidget::class.java, R.string.widget_clock, R.string.widget_clock_desc, 250, 110),
    ClockAnalog(ClockAnalogWidget::class.java, R.string.widget_clock_analog, R.string.widget_clock_analog_desc, 250, 110),
    Weather(WeatherWidget::class.java, R.string.widget_weather, R.string.widget_weather_desc, 250, 110),
    Battery(BatteryWidget::class.java, R.string.widget_battery, R.string.widget_battery_desc, 250, 110),
    Music(MusicWidget::class.java, R.string.widget_music, R.string.widget_music_desc, 250, 110),
    Calendar(CalendarWidget::class.java, R.string.widget_calendar, R.string.widget_calendar_desc, 250, 110),
    Notes(NotesWidget::class.java, R.string.widget_notes, R.string.widget_notes_desc, 250, 110);

    companion object {
        fun of(provider: Class<*>?): WidgetKind? = entries.firstOrNull { it.provider == provider }
    }
}

/** Text colours chosen from how bright the rendered glass is. */
class Palette(val primary: Int, val secondary: Int) {
    companion object {
        val Light = Palette(0xFFFFFFFF.toInt(), 0xB3FFFFFF.toInt())
        val Dark = Palette(0xFF14161B.toInt(), 0xB314161B.toInt())
        fun forLightGlass(isLight: Boolean) = if (isLight) Dark else Light
    }
}
