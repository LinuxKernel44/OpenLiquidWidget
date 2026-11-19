package io.github.linuxkernel44.openliquidwidget.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import io.github.linuxkernel44.openliquidwidget.ui.WidgetConfigActivity
import android.widget.RemoteViews
import io.github.linuxkernel44.openliquidwidget.R
import io.github.linuxkernel44.openliquidwidget.data.AppPrefs
import io.github.linuxkernel44.openliquidwidget.data.Weather
import io.github.linuxkernel44.openliquidwidget.data.WeatherApi

class WeatherWidget : GlassWidgetProvider(WidgetKind.Weather) {
    override val layoutRes = R.layout.widget_weather

    override fun bind(ctx: Context, views: RemoteViews, id: Int, palette: Palette) {
        val prefs = AppPrefs(ctx)
        val city = prefs.city(id)
        views.setTextColor(R.id.w_city, palette.primary)
        views.setTextColor(R.id.w_temp, palette.primary)
        views.setTextColor(R.id.w_desc, palette.secondary)
        views.setTextColor(R.id.w_range, palette.secondary)
        if (city == null) {
            views.setTextViewText(R.id.w_city, ctx.getString(R.string.weather_pick_city))
            views.setTextViewText(R.id.w_temp, "--°")
            views.setTextViewText(R.id.w_desc, "")
            views.setTextViewText(R.id.w_icon, "🌡️")
            views.setTextViewText(R.id.w_range, "")
            return
        }
        val cached = prefs.weatherCache(id)
        val stale = cached == null || System.currentTimeMillis() - cached.first > 20 * 60_000L
        var weather = cached?.let { Weather.fromJson(it.second) }
        if (stale) {
            WeatherApi.forecast(city, prefs.fahrenheit)?.let {
                weather = it
                prefs.setWeatherCache(id, it.toJson())
            }
        }
        views.setTextViewText(R.id.w_city, city.name.substringBefore(','))
        val w = weather
        if (w == null) {
            views.setTextViewText(R.id.w_temp, "--°")
            views.setTextViewText(R.id.w_desc, ctx.getString(R.string.weather_unavailable))
            views.setTextViewText(R.id.w_icon, "🌡️")
            views.setTextViewText(R.id.w_range, "")
        } else {
            views.setTextViewText(R.id.w_temp, "${w.temp}°")
            views.setTextViewText(R.id.w_desc, w.description)
            views.setTextViewText(R.id.w_icon, w.emoji)
            views.setTextViewText(R.id.w_range, "H ${w.high}°  L ${w.low}°")
        }
    }

    // Without a city, tapping the widget opens its configurator (city search) instead of the app.
    override fun clickIntent(ctx: Context, id: Int): PendingIntent =
        if (AppPrefs(ctx).city(id) != null) super.clickIntent(ctx, id)
        else PendingIntent.getActivity(
            ctx, id, Intent(ctx, WidgetConfigActivity::class.java).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
}
