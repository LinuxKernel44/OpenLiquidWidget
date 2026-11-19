package io.github.linuxkernel44.openliquidwidget.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.linuxkernel44.openliquidwidget.R
import io.github.linuxkernel44.openliquidwidget.data.AppPrefs
import io.github.linuxkernel44.openliquidwidget.widget.GlassWidgetProvider
import io.github.linuxkernel44.openliquidwidget.widget.WidgetKind
import kotlin.concurrent.thread

/** Full-screen editor for the single note shown by the Notes widget. */
class NoteEditActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = AppPrefs(this)
        setContent {
            MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
                Surface(Modifier.fillMaxSize()) {
                    var text by remember { mutableStateOf(prefs.note) }
                    Column(Modifier.statusBarsPadding().imePadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(R.string.note_title), style = MaterialTheme.typography.titleLarge)
                        OutlinedTextField(text, { text = it }, Modifier.fillMaxWidth().weight(1f))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton({ finish() }) { Text("Cancel") }
                            Button({
                                prefs.note = text
                                thread { GlassWidgetProvider.updateKind(applicationContext, WidgetKind.Notes) }
                                finish()
                            }) { Text(stringResource(R.string.save)) }
                        }
                    }
                }
            }
        }
    }
}
