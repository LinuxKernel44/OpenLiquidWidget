package io.github.linuxkernel44.openliquidwidget.widget

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import android.text.format.Formatter
import android.widget.RemoteViews
import io.github.linuxkernel44.openliquidwidget.R

class BatteryWidget : GlassWidgetProvider(WidgetKind.Battery) {
    override val layoutRes = R.layout.widget_battery

    override fun bind(ctx: Context, views: RemoteViews, id: Int, palette: Palette) {
        val bi = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = bi?.let { it.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) * 100 / it.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1) } ?: 0
        val status = bi?.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

        val mem = ActivityManager.MemoryInfo().also { (ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(it) }
        val ramUsed = mem.totalMem - mem.availMem
        val stat = StatFs(Environment.getDataDirectory().path)
        val total = stat.totalBytes
        val used = total - stat.availableBytes

        fun row(key: String, label: String, pct: Int, value: String, barId: Int, labelId: Int, valueId: Int) {
            views.setTextViewText(labelId, label)
            views.setTextViewText(valueId, value)
            views.setTextColor(labelId, palette.primary)
            views.setTextColor(valueId, palette.secondary)
            views.setProgressBar(barId, 100, pct.coerceIn(0, 100), false)
            views.setColorStateList(barId, "setProgressTintList", ColorStateList.valueOf(palette.primary))
        }
        row("battery", ctx.getString(R.string.battery_label), level, "$level%" + if (charging) " ⚡" else "", R.id.b_battery_bar, R.id.b_battery_label, R.id.b_battery_value)
        row("ram", ctx.getString(R.string.ram_label), (ramUsed * 100 / mem.totalMem).toInt(), "${gb(ctx, ramUsed)}/${gb(ctx, mem.totalMem)}", R.id.b_ram_bar, R.id.b_ram_label, R.id.b_ram_value)
        row("storage", ctx.getString(R.string.storage_label), (used * 100 / total).toInt(), "${gb(ctx, used)}/${gb(ctx, total)}", R.id.b_storage_bar, R.id.b_storage_label, R.id.b_storage_value)
        views.setTextViewText(R.id.b_net, network(ctx))
        views.setTextColor(R.id.b_net, palette.secondary)
    }

    private fun gb(ctx: Context, bytes: Long): String = Formatter.formatShortFileSize(ctx, bytes).replace(" ", "")

    private fun network(ctx: Context): String {
        val cm = ctx.getSystemService(ConnectivityManager::class.java)
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return "📵 " + ctx.getString(R.string.offline)
        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "📶 Wi-Fi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "📶 Mobile data"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "🔌 Ethernet"
            else -> "🌐 Connected"
        }
    }
}
