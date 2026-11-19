package io.github.linuxkernel44.openliquidwidget.widget

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import android.widget.RemoteViews
import io.github.linuxkernel44.openliquidwidget.R

class ClockWidget : GlassWidgetProvider(WidgetKind.Clock) {
    override val layoutRes = R.layout.widget_clock

    override fun bind(ctx: Context, views: RemoteViews, id: Int, palette: Palette) {
        views.setTextColor(R.id.clock_time, palette.primary)
        views.setTextColor(R.id.clock_date, palette.secondary)
    }

    override fun clickIntent(ctx: Context, id: Int): PendingIntent = PendingIntent.getActivity(
        ctx, id, Intent(AlarmClock.ACTION_SHOW_ALARMS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}
