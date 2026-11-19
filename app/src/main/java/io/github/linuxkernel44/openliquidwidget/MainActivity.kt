package io.github.linuxkernel44.openliquidwidget

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import io.github.linuxkernel44.openliquidwidget.data.AppPrefs
import io.github.linuxkernel44.openliquidwidget.data.GlassParams
import io.github.linuxkernel44.openliquidwidget.data.WallpaperMode
import io.github.linuxkernel44.openliquidwidget.glass.WallpaperSource
import io.github.linuxkernel44.openliquidwidget.ui.GlassButton
import io.github.linuxkernel44.openliquidwidget.ui.WidgetConfigActivity
import io.github.linuxkernel44.openliquidwidget.ui.WidgetContent
import io.github.linuxkernel44.openliquidwidget.ui.liquidGlass
import io.github.linuxkernel44.openliquidwidget.widget.GlassWidgetProvider
import io.github.linuxkernel44.openliquidwidget.widget.MusicState
import io.github.linuxkernel44.openliquidwidget.widget.Palette
import io.github.linuxkernel44.openliquidwidget.widget.RefreshWorker
import io.github.linuxkernel44.openliquidwidget.widget.WidgetKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.concurrent.thread

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        RefreshWorker.schedule(this)
        setContent { MaterialTheme(colorScheme = darkColorScheme()) { Gallery() } }
    }

    override fun onResume() {
        super.onResume()
        thread { runCatching { GlassWidgetProvider.updateAll(applicationContext) } }
    }
}

@Composable
private fun Gallery() {
    val ctx = LocalContext.current
    val prefs = remember { AppPrefs(ctx) }
    var revision by remember { mutableIntStateOf(0) }
    var tick by remember { mutableIntStateOf(0) } // bumped on resume so permission status refreshes
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val o = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) tick++ }
        owner.lifecycle.addObserver(o)
        onDispose { owner.lifecycle.removeObserver(o) }
    }
    val wallpaper by produceState(null as androidx.compose.ui.graphics.ImageBitmap?, revision) {
        value = withContext(Dispatchers.IO) { WallpaperSource.load(ctx).asImageBitmap() }
    }
    val backdrop = rememberLayerBackdrop()

    Box(Modifier.fillMaxSize().background(Color(0xFF0B1030))) {
        wallpaper?.let { Image(it, null, Modifier.fillMaxSize().layerBackdrop(backdrop), contentScale = ContentScale.Crop) }
        LazyColumn(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Column(Modifier.padding(top = 8.dp, bottom = 4.dp)) {
                    Text(stringResource(R.string.app_name), color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.SemiBold)
                    Text("Liquid glass widgets for your home screen", color = Color.White.copy(alpha = 0.75f), fontSize = 15.sp)
                }
            }
            item { WallpaperCard(backdrop, prefs, tick) { revision++ } }
            item { PermissionsCard(backdrop, tick) }
            items(WidgetKind.entries) { WidgetCard(it, backdrop) }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun SectionCard(backdrop: com.kyant.backdrop.Backdrop, title: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().liquidGlass(backdrop).padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        content()
    }
}

@Composable
private fun WallpaperCard(backdrop: com.kyant.backdrop.Backdrop, prefs: AppPrefs, tick: Int, onChanged: () -> Unit) {
    val ctx = LocalContext.current
    var mode by remember { mutableStateOfMode(prefs) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) thread {
            if (WallpaperSource.importCustom(ctx, uri)) {
                prefs.wallpaperMode = WallpaperMode.Custom
                prefs.wallpaperRevision = System.currentTimeMillis()
                GlassWidgetProvider.updateAll(ctx)
                (ctx as android.app.Activity).runOnUiThread { mode = WallpaperMode.Custom; onChanged() }
            }
        }
    }
    val systemOk by produceState(true, tick, mode) { value = withContext(Dispatchers.IO) { WallpaperSource.systemReadable(ctx) } }
    SectionCard(backdrop, "Backdrop") {
        Text("The glass refracts this image. Android only lets apps read the system wallpaper with “All files access”.", color = Color.White.copy(alpha = 0.75f), fontSize = 13.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for ((m, label) in listOf(WallpaperMode.Auto to "System", WallpaperMode.Custom to "Image", WallpaperMode.Generated to "Mesh")) {
                GlassButton(if (mode == m) "● $label" else label, backdrop) {
                    if (m == WallpaperMode.Custom && !java.io.File(ctx.filesDir, "wallpaper.jpg").exists()) {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    } else {
                        mode = m; prefs.wallpaperMode = m; prefs.wallpaperRevision = System.currentTimeMillis(); onChanged()
                        thread { GlassWidgetProvider.updateAll(ctx) }
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlassButton("Choose image", backdrop) { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
            if (mode == WallpaperMode.Auto && !systemOk) GlassButton("Allow all files", backdrop) {
                ctx.startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:${ctx.packageName}")))
            }
        }
        if (mode == WallpaperMode.Auto && !systemOk) Text("System wallpaper not readable yet — using the mesh backdrop.", color = Color(0xFFFFD27A), fontSize = 13.sp)
    }
}

private fun mutableStateOfMode(prefs: AppPrefs) = androidx.compose.runtime.mutableStateOf(prefs.wallpaperMode)

@Composable
private fun PermissionsCard(backdrop: com.kyant.backdrop.Backdrop, tick: Int) {
    val ctx = LocalContext.current
    val calendarLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { thread { GlassWidgetProvider.updateAll(ctx) } }
    val calendar = remember(tick) { ctx.checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED }
    val music = remember(tick) { MusicState.hasAccess(ctx) }
    SectionCard(backdrop, "Permissions") {
        Text("Only needed by the widgets that use them. Nothing leaves your device except weather lookups.", color = Color.White.copy(alpha = 0.75f), fontSize = 13.sp)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Calendar · " + if (calendar) "allowed" else "off", color = Color.White, fontSize = 15.sp)
            if (!calendar) GlassButton("Allow", backdrop) { calendarLauncher.launch(Manifest.permission.READ_CALENDAR) }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Music (notification access) · " + if (music) "allowed" else "off", color = Color.White, fontSize = 15.sp)
            if (!music) GlassButton("Allow", backdrop) { ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
        }
    }
}

@Composable
private fun WidgetCard(kind: WidgetKind, backdrop: com.kyant.backdrop.Backdrop) {
    val ctx = LocalContext.current
    val p = GlassParams()
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.fillMaxWidth().height(120.dp).liquidGlass(backdrop, corner = p.cornerDp.dp, blurDp = p.blur)) {
            WidgetContent(kind, Palette.Light)
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(kind.title), color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Text(stringResource(kind.description), color = Color.White.copy(alpha = 0.75f), fontSize = 13.sp)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlassButton("Add to home screen", backdrop) { pin(ctx, kind) }
            GlassButton("Customize", backdrop) {
                ctx.startActivity(Intent(ctx, WidgetConfigActivity::class.java).putExtra(WidgetConfigActivity.EXTRA_KIND, kind.name))
            }
        }
    }
}

private fun pin(ctx: android.content.Context, kind: WidgetKind) {
    val manager = AppWidgetManager.getInstance(ctx)
    if (manager.isRequestPinAppWidgetSupported) manager.requestPinAppWidget(ComponentName(ctx, kind.provider), null, null)
    else android.widget.Toast.makeText(ctx, "Your launcher can't pin widgets from apps. Long-press the home screen instead.", android.widget.Toast.LENGTH_LONG).show()
}
