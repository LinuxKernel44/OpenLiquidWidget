package io.github.linuxkernel44.openliquidwidget.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.TypedValue
import android.widget.RemoteViews
import io.github.linuxkernel44.openliquidwidget.MainActivity
import io.github.linuxkernel44.openliquidwidget.R
import io.github.linuxkernel44.openliquidwidget.data.AppPrefs
import io.github.linuxkernel44.openliquidwidget.data.GlassStore
import io.github.linuxkernel44.openliquidwidget.glass.GlassRenderer
import io.github.linuxkernel44.openliquidwidget.glass.WallpaperSource
import kotlin.concurrent.thread

/**
 * Base class of every widget: renders the glass background for the widget's current size, lets the subclass fill in the content
 * views, and pushes the result. Rendering happens on a worker thread under `goAsync()`.
 */
abstract class GlassWidgetProvider(val kind: WidgetKind) : AppWidgetProvider() {
    abstract val layoutRes: Int

    /** Fills the content views of [views]. Runs on a worker thread, so it may do (short) blocking I/O. */
    abstract fun bind(ctx: Context, views: RemoteViews, id: Int, palette: Palette)

    /** Where tapping the widget goes. Defaults to the app. */
    open fun clickIntent(ctx: Context, id: Int): PendingIntent = PendingIntent.getActivity(
        ctx, id, Intent(ctx, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = async(context) { ids.forEach { update(context, manager, it) } }

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, newOptions: Bundle) =
        async(context) { update(context, manager, id) }

    override fun onEnabled(context: Context) = RefreshWorker.schedule(context)

    override fun onDeleted(context: Context, ids: IntArray) {
        val glass = GlassStore(context)
        val prefs = AppPrefs(context)
        ids.forEach { glass.remove(it); prefs.removeWidget(it) }
    }

    private fun async(context: Context, block: () -> Unit) {
        val pending = goAsync()
        thread(name = "glass-widget") {
            try { block() } catch (t: Throwable) { android.util.Log.e("OpenLiquidWidget", "update failed", t) } finally { pending.finish() }
        }
    }

    /** Renders and pushes one widget instance. Blocking; call off the main thread. */
    fun update(ctx: Context, manager: AppWidgetManager, id: Int) {
        val dm = ctx.resources.displayMetrics
        val (wDp, hDp) = sizeDp(ctx, manager, id)
        val params = GlassStore(ctx).get(id)
        val wPx = (wDp * dm.density).toInt().coerceAtLeast(64)
        val hPx = (hDp * dm.density).toInt().coerceAtLeast(64)
        val glass = GlassRenderer.render(WallpaperSource.load(ctx), wPx, hPx, dm.density, params, AppPrefs(ctx).glassQuality)
        val views = RemoteViews(ctx.packageName, layoutRes)
        views.setImageViewBitmap(R.id.glass_bg, glass.bitmap)
        bind(ctx, views, id, Palette.forLightGlass(glass.isLight))
        views.setOnClickPendingIntent(R.id.root, clickIntent(ctx, id))
        manager.updateAppWidget(id, views)
    }

    private fun sizeDp(ctx: Context, manager: AppWidgetManager, id: Int): Pair<Int, Int> {
        val o = manager.getAppWidgetOptions(id)
        val portrait = ctx.resources.configuration.orientation != Configuration.ORIENTATION_LANDSCAPE
        val w = if (portrait) o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH) else o.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH)
        val h = if (portrait) o.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT) else o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
        return (if (w > 0) w else kind.defaultWidthDp) to (if (h > 0) h else kind.defaultHeightDp)
    }

    protected fun RemoteViews.setSp(viewId: Int, sp: Float) = setTextViewTextSize(viewId, TypedValue.COMPLEX_UNIT_SP, sp)

    companion object {
        /** Re-renders every placed widget (all kinds). Blocking. */
        fun updateAll(ctx: Context) {
            val manager = AppWidgetManager.getInstance(ctx)
            for (kind in WidgetKind.entries) {
                val ids = manager.getAppWidgetIds(ComponentName(ctx, kind.provider))
                if (ids.isEmpty()) continue
                val p = kind.provider.getDeclaredConstructor().newInstance() as GlassWidgetProvider
                ids.forEach { runCatching { p.update(ctx, manager, it) } }
            }
        }

        /** Re-renders the placed widgets of one kind. Blocking. */
        fun updateKind(ctx: Context, kind: WidgetKind) {
            val manager = AppWidgetManager.getInstance(ctx)
            val ids = manager.getAppWidgetIds(ComponentName(ctx, kind.provider))
            val p = kind.provider.getDeclaredConstructor().newInstance() as GlassWidgetProvider
            ids.forEach { runCatching { p.update(ctx, manager, it) } }
        }
    }
}
