package io.github.linuxkernel44.openliquidwidget.ui

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.linuxkernel44.openliquidwidget.data.AppPrefs
import io.github.linuxkernel44.openliquidwidget.data.City
import io.github.linuxkernel44.openliquidwidget.data.GlassParams
import io.github.linuxkernel44.openliquidwidget.data.GlassStore
import io.github.linuxkernel44.openliquidwidget.data.WeatherApi
import io.github.linuxkernel44.openliquidwidget.glass.GlassRenderer
import io.github.linuxkernel44.openliquidwidget.glass.GlassResult
import io.github.linuxkernel44.openliquidwidget.glass.WallpaperSource
import io.github.linuxkernel44.openliquidwidget.widget.GlassWidgetProvider
import io.github.linuxkernel44.openliquidwidget.widget.Palette
import io.github.linuxkernel44.openliquidwidget.widget.WidgetKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.concurrent.thread

/**
 * Configuration screen. Launched by the home screen when a widget is added or reconfigured (carries `EXTRA_APPWIDGET_ID`), or by
 * the gallery (carries [EXTRA_KIND]) to edit the default look shared by widgets without their own settings.
 */
class WidgetConfigActivity : ComponentActivity() {
    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        val fromLauncher = widgetId != AppWidgetManager.INVALID_APPWIDGET_ID
        setResult(RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))

        val kind = if (fromLauncher) {
            WidgetKind.of(runCatching { Class.forName(AppWidgetManager.getInstance(this).getAppWidgetInfo(widgetId).provider.className) }.getOrNull())
        } else {
            runCatching { WidgetKind.valueOf(intent.getStringExtra(EXTRA_KIND) ?: "") }.getOrNull()
        }
        if (kind == null) { finish(); return }
        val storeId = if (fromLauncher) widgetId else GlassStore.DEFAULTS_ID

        setContent {
            MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
                Surface(Modifier.fillMaxSize()) {
                    ConfigScreen(kind, storeId, onDone = {
                        setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
                        thread { runCatching { GlassWidgetProvider.updateAll(applicationContext) } }
                        finish()
                    }, onCancel = { finish() })
                }
            }
        }
    }

    companion object {
        const val EXTRA_KIND = "kind"
    }
}

private val Swatches = listOf(0xFFFFFFFF, 0xFF000000, 0xFF4F8CFF, 0xFFFF5C9A, 0xFF37D6A0, 0xFFFFB347).map { it.toInt() }

@Composable
private fun ConfigScreen(kind: WidgetKind, storeId: Int, onDone: () -> Unit, onCancel: () -> Unit) {
    val ctx = LocalContext.current
    val store = remember { GlassStore(ctx) }
    val prefs = remember { AppPrefs(ctx) }
    var params by remember { mutableStateOf(store.get(storeId)) }
    val density = ctx.resources.displayMetrics.density

    val rendered by produceState<GlassResult?>(null, params) {
        delay(60) // debounce slider drags: produceState restarts (and cancels) on every change
        value = withContext(Dispatchers.Default) {
            runCatching {
                GlassRenderer.render(WallpaperSource.load(ctx), (kind.defaultWidthDp * density).toInt(), (kind.defaultHeightDp * density).toInt(), density, params)
            }.getOrNull()
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
        Box(
            Modifier.fillMaxWidth().padding(16.dp).aspectRatio(kind.defaultWidthDp / kind.defaultHeightDp.toFloat()),
            contentAlignment = Alignment.Center,
        ) {
            val r = rendered
            if (r != null) {
                Image(r.bitmap.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
                WidgetContent(kind, Palette.forLightGlass(r.isLight))
            }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(if (storeId == GlassStore.DEFAULTS_ID) "Default look" else "This widget", style = MaterialTheme.typography.titleMedium)
            Labeled("Blur", params.blur, 0f..24f) { params = params.copy(blur = it) }
            Labeled("Refraction", params.refraction, 0f..1f) { params = params.copy(refraction = it) }
            Labeled("Tint", params.tintAlpha, 0f..0.6f) { params = params.copy(tintAlpha = it) }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(vertical = 6.dp)) {
                for (c in Swatches) Box(
                    Modifier.size(32.dp).clip(CircleShape).background(Color(c))
                        .border(if (c == params.tintColor) 3.dp else 1.dp, if (c == params.tintColor) MaterialTheme.colorScheme.primary else Color.Gray, CircleShape)
                        .clickable { params = params.copy(tintColor = c) },
                )
            }
            Labeled("Corner radius", params.cornerDp, 8f..48f) { params = params.copy(cornerDp = it) }
            Labeled("Edge highlight", params.rim, 0f..1f) { params = params.copy(rim = it) }
            Text("Wallpaper position — set where you'll put the widget so the glass shows what is behind it.", style = MaterialTheme.typography.bodySmall)
            Labeled("Horizontal", params.posX, 0f..1f) { params = params.copy(posX = it) }
            Labeled("Vertical", params.posY, 0f..1f) { params = params.copy(posY = it) }
            if (kind == WidgetKind.Weather) WeatherSettings(prefs, storeId)
        }
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.End) {
            TextButton(onCancel) { Text("Cancel") }
            Button({ store.save(storeId, params); onDone() }) { Text("Done") }
        }
    }
}

@Composable
private fun Labeled(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(0.38f), style = MaterialTheme.typography.bodyMedium)
        Slider(value, onChange, Modifier.weight(0.62f), valueRange = range)
    }
}

@Composable
private fun WeatherSettings(prefs: AppPrefs, storeId: Int) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf(emptyList<City>()) }
    var current by remember { mutableStateOf(prefs.city(storeId)) }
    var fahrenheit by remember { mutableStateOf(prefs.fahrenheit) }
    Text("Weather", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
    Text("City: ${current?.name ?: "none yet"}", style = MaterialTheme.typography.bodyMedium)
    OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), label = { Text("Search a city") }, singleLine = true)
    LaunchedEffect(query) {
        if (query.length < 2) { results = emptyList(); return@LaunchedEffect }
        delay(350)
        results = withContext(Dispatchers.IO) { WeatherApi.search(query) }
    }
    for (c in results) Text(
        c.name,
        Modifier.fillMaxWidth().clickable {
            prefs.setCity(storeId, c); prefs.clearWeatherCache(storeId)
            // A city chosen while editing the defaults also applies to widgets that have none of their own.
            current = c; results = emptyList(); query = ""
            scope.launch { /* keep the picker responsive; the refresh happens when the screen closes */ }
        }.padding(vertical = 10.dp),
        style = MaterialTheme.typography.bodyLarge,
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Fahrenheit", Modifier.weight(1f))
        Switch(fahrenheit, { fahrenheit = it; prefs.fahrenheit = it })
    }
}
