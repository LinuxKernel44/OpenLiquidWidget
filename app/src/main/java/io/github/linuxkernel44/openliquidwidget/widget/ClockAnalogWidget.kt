package io.github.linuxkernel44.openliquidwidget.widget

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import android.view.View
import android.widget.RemoteViews
import io.github.linuxkernel44.openliquidwidget.R

class ClockAnalogWidget : GlassWidgetProvider(WidgetKind.ClockAnalog) {
    override val layoutRes = R.layout.widget_clock_analog

    override fun bind(ctx: Context, views: RemoteViews, id: Int, palette: Palette) {
        // Two stacked dials (white / dark hands): the visible one follows the glass brightness.
        val dark = palette.primary == Palette.Dark.primary
        views.setViewVisibility(R.id.ac_light, if (dark) View.GONE else View.VISIBLE)
        views.setViewVisibility(R.id.ac_dark, if (dark) View.VISIBLE else View.GONE)
        views.setTextColor(R.id.ac_weekday, palette.primary)
        views.setTextColor(R.id.ac_date, palette.secondary)
    }

    override fun clickIntent(ctx: Context, id: Int): PendingIntent = PendingIntent.getActivity(
        ctx, id, Intent(AlarmClock.ACTION_SHOW_ALARMS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}
