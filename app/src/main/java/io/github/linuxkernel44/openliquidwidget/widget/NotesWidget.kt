package io.github.linuxkernel44.openliquidwidget.widget

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import io.github.linuxkernel44.openliquidwidget.R
import io.github.linuxkernel44.openliquidwidget.data.AppPrefs
import io.github.linuxkernel44.openliquidwidget.ui.NoteEditActivity

class NotesWidget : GlassWidgetProvider(WidgetKind.Notes) {
    override val layoutRes = R.layout.widget_notes

    override fun bind(ctx: Context, views: RemoteViews, id: Int, palette: Palette) {
        val note = AppPrefs(ctx).note
        views.setTextViewText(R.id.n_header, ctx.getString(R.string.notes_header))
        views.setTextColor(R.id.n_header, palette.secondary)
        views.setTextViewText(R.id.n_text, note.ifBlank { ctx.getString(R.string.notes_empty) })
        views.setTextColor(R.id.n_text, if (note.isBlank()) palette.secondary else palette.primary)
    }

    override fun clickIntent(ctx: Context, id: Int): PendingIntent = PendingIntent.getActivity(
        ctx, id, Intent(ctx, NoteEditActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}
