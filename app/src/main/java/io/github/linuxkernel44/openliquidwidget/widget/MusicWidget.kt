package io.github.linuxkernel44.openliquidwidget.widget

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.provider.Settings
import android.widget.RemoteViews
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.edit
import androidx.core.graphics.createBitmap
import io.github.linuxkernel44.openliquidwidget.R
import java.io.File

/** What the music widget shows; persisted so it survives process death. */
object MusicState {
    private fun prefs(ctx: Context) = ctx.applicationContext.getSharedPreferences("music", Context.MODE_PRIVATE)
    private fun artFile(ctx: Context) = File(ctx.filesDir, "music_art.png")

    data class Snapshot(val title: String, val artist: String, val playing: Boolean, val art: Bitmap?)

    fun read(ctx: Context): Snapshot? {
        val p = prefs(ctx)
        val title = p.getString("title", null) ?: return null
        val art = artFile(ctx).takeIf { p.getBoolean("hasArt", false) && it.exists() }?.let { BitmapFactory.decodeFile(it.path) }
        return Snapshot(title, p.getString("artist", "") ?: "", p.getBoolean("playing", false), art)
    }

    fun write(ctx: Context, title: String?, artist: String?, playing: Boolean, art: Bitmap?) {
        if (title == null) { prefs(ctx).edit { clear() }; return }
        val scaled = art?.let { Bitmap.createScaledBitmap(it, 192, 192, true) }
        if (scaled != null) artFile(ctx).outputStream().use { scaled.compress(Bitmap.CompressFormat.PNG, 100, it) }
        prefs(ctx).edit {
            putString("title", title); putString("artist", artist ?: ""); putBoolean("playing", playing); putBoolean("hasArt", scaled != null)
        }
    }

    fun hasAccess(ctx: Context): Boolean = NotificationManagerCompat.getEnabledListenerPackages(ctx).contains(ctx.packageName)
}

class MusicWidget : GlassWidgetProvider(WidgetKind.Music) {
    override val layoutRes = R.layout.widget_music

    override fun bind(ctx: Context, views: RemoteViews, id: Int, palette: Palette) {
        val access = MusicState.hasAccess(ctx)
        val s = if (access) MusicState.read(ctx) else null
        views.setTextColor(R.id.m_title, palette.primary)
        views.setTextColor(R.id.m_artist, palette.secondary)
        views.setTextViewText(R.id.m_title, s?.title ?: ctx.getString(R.string.music_nothing))
        views.setTextViewText(R.id.m_artist, s?.artist ?: if (access) "" else ctx.getString(R.string.music_need_access))
        views.setImageViewBitmap(R.id.m_art, rounded(s?.art ?: placeholder(), 18f))
        views.setImageViewResource(R.id.m_play, if (s?.playing == true) R.drawable.ic_pause else R.drawable.ic_play)
        for (v in intArrayOf(R.id.m_prev, R.id.m_play, R.id.m_next)) views.setInt(v, "setColorFilter", palette.primary)
        views.setOnClickPendingIntent(R.id.m_prev, action(ctx, MusicActionReceiver.PREV))
        views.setOnClickPendingIntent(R.id.m_play, action(ctx, MusicActionReceiver.TOGGLE))
        views.setOnClickPendingIntent(R.id.m_next, action(ctx, MusicActionReceiver.NEXT))
    }

    // The root tap opens the notification-access settings until access is granted, otherwise the app.
    override fun clickIntent(ctx: Context, id: Int): PendingIntent =
        if (MusicState.hasAccess(ctx)) super.clickIntent(ctx, id)
        else PendingIntent.getActivity(ctx, id, Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    private fun action(ctx: Context, a: String) = PendingIntent.getBroadcast(
        ctx, a.hashCode(), Intent(ctx, MusicActionReceiver::class.java).setAction(a), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun placeholder(): Bitmap {
        val b = createBitmap(96, 96)
        Canvas(b).drawColor(0x33FFFFFF)
        return b
    }

    private fun rounded(src: Bitmap, radiusDp: Float): Bitmap {
        val out = createBitmap(src.width, src.height)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val r = radiusDp / 76f * src.width
        Canvas(out).apply {
            drawRoundRect(RectF(0f, 0f, src.width.toFloat(), src.height.toFloat()), r, r, p)
            p.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN)
            drawBitmap(src, 0f, 0f, p)
        }
        return out
    }
}

/** Handles the transport buttons of the music widget. */
class MusicActionReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val controller = activeController(context) ?: return
        when (intent.action) {
            PREV -> controller.transportControls.skipToPrevious()
            NEXT -> controller.transportControls.skipToNext()
            TOGGLE -> if (controller.playbackState?.state == PlaybackState.STATE_PLAYING) controller.transportControls.pause() else controller.transportControls.play()
        }
    }

    companion object {
        const val PREV = "io.github.linuxkernel44.openliquidwidget.PREV"
        const val NEXT = "io.github.linuxkernel44.openliquidwidget.NEXT"
        const val TOGGLE = "io.github.linuxkernel44.openliquidwidget.TOGGLE"

        fun activeController(ctx: Context): MediaController? = runCatching {
            val msm = ctx.getSystemService(MediaSessionManager::class.java)
            val list = msm.getActiveSessions(ComponentName(ctx, MediaListenerService::class.java))
            list.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING } ?: list.firstOrNull()
        }.getOrNull()
    }
}

/**
 * Notification-listener service whose only job is to unlock `MediaSessionManager.getActiveSessions`: it watches the active media
 * sessions and keeps [MusicState] (and the widgets) up to date. It reads no notification content.
 */
class MediaListenerService : android.service.notification.NotificationListenerService() {
    private val callbacks = HashMap<MediaController, MediaController.Callback>()
    private val sessionListener = MediaSessionManager.OnActiveSessionsChangedListener { track(it ?: emptyList()) }

    override fun onListenerConnected() {
        val msm = getSystemService(MediaSessionManager::class.java)
        val me = ComponentName(this, MediaListenerService::class.java)
        runCatching {
            msm.addOnActiveSessionsChangedListener(sessionListener, me)
            track(msm.getActiveSessions(me))
        }
    }

    override fun onListenerDisconnected() {
        runCatching { getSystemService(MediaSessionManager::class.java).removeOnActiveSessionsChangedListener(sessionListener) }
        callbacks.forEach { (c, cb) -> c.unregisterCallback(cb) }
        callbacks.clear()
    }

    private fun track(controllers: List<MediaController>) {
        callbacks.keys.filter { it !in controllers }.forEach { callbacks.remove(it)?.let { cb -> it.unregisterCallback(cb) } }
        for (c in controllers) if (c !in callbacks) {
            val cb = object : MediaController.Callback() {
                override fun onMetadataChanged(metadata: MediaMetadata?) = publish()
                override fun onPlaybackStateChanged(state: PlaybackState?) = publish()
            }
            c.registerCallback(cb)
            callbacks[c] = cb
        }
        publish()
    }

    private fun publish() {
        val c = callbacks.keys.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING } ?: callbacks.keys.firstOrNull()
        val md = c?.metadata
        val title = md?.getString(MediaMetadata.METADATA_KEY_TITLE)
        if (c == null || title == null) MusicState.write(this, null, null, false, null)
        else MusicState.write(
            this, title, md.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: md.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST),
            c.playbackState?.state == PlaybackState.STATE_PLAYING,
            md.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) ?: md.getBitmap(MediaMetadata.METADATA_KEY_ART),
        )
        kotlin.concurrent.thread { GlassWidgetProvider.updateKind(applicationContext, WidgetKind.Music) }
    }
}
