package io.github.linuxkernel44.openliquidwidget.widget

import android.Manifest
import android.app.PendingIntent
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.CalendarContract
import android.text.format.DateUtils
import android.widget.RemoteViews
import io.github.linuxkernel44.openliquidwidget.MainActivity
import io.github.linuxkernel44.openliquidwidget.R

class CalendarWidget : GlassWidgetProvider(WidgetKind.Calendar) {
    override val layoutRes = R.layout.widget_calendar

    private val rows = intArrayOf(R.id.c_e0, R.id.c_e1, R.id.c_e2)

    override fun bind(ctx: Context, views: RemoteViews, id: Int, palette: Palette) {
        views.setTextViewText(R.id.c_header, ctx.getString(R.string.calendar_header))
        views.setTextColor(R.id.c_header, palette.secondary)
        rows.forEach { views.setTextViewText(it, ""); views.setTextColor(it, palette.primary) }
        if (ctx.checkSelfPermission(Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            views.setTextViewText(R.id.c_e0, ctx.getString(R.string.calendar_no_permission))
            return
        }
        val events = upcoming(ctx)
        if (events.isEmpty()) {
            views.setTextViewText(R.id.c_e0, ctx.getString(R.string.calendar_no_events))
            return
        }
        events.take(rows.size).forEachIndexed { i, e -> views.setTextViewText(rows[i], e) }
    }

    private fun upcoming(ctx: Context): List<String> {
        val now = System.currentTimeMillis()
        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, now)
        ContentUris.appendId(builder, now + 7 * DateUtils.DAY_IN_MILLIS)
        val out = mutableListOf<String>()
        runCatching {
            ctx.contentResolver.query(
                builder.build(),
                arrayOf(CalendarContract.Instances.TITLE, CalendarContract.Instances.BEGIN, CalendarContract.Instances.END, CalendarContract.Instances.ALL_DAY),
                null, null, "${CalendarContract.Instances.BEGIN} ASC",
            )?.use { c ->
                while (c.moveToNext() && out.size < 3) {
                    val end = c.getLong(2)
                    if (end <= now) continue
                    val begin = c.getLong(1)
                    val allDay = c.getInt(3) == 1
                    val flags = if (allDay) DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_ALL
                    else DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_ABBREV_ALL
                    // Event instances are stored in UTC for all-day events; the label is date-only so that is fine.
                    val whenText = if (allDay) DateUtils.formatDateTime(ctx, begin, flags or DateUtils.FORMAT_UTC) else DateUtils.formatDateTime(ctx, begin, flags)
                    out += "$whenText  ${c.getString(0) ?: ""}"
                }
            }
        }
        return out
    }

    override fun clickIntent(ctx: Context, id: Int): PendingIntent {
        val granted = ctx.checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
        val intent = if (granted) Intent(Intent.ACTION_VIEW, CalendarContract.CONTENT_URI.buildUpon().appendPath("time").build()) else Intent(ctx, MainActivity::class.java)
        return PendingIntent.getActivity(ctx, id, intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }
}
