package io.github.linuxkernel44.openliquidwidget.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.linuxkernel44.openliquidwidget.widget.Palette
import io.github.linuxkernel44.openliquidwidget.widget.WidgetKind

/** Compose rendition of each widget's content with sample data, used for the gallery and the configurator previews. */
@Composable
fun WidgetContent(kind: WidgetKind, palette: Palette, modifier: Modifier = Modifier) {
    val primary = Color(palette.primary)
    val secondary = Color(palette.secondary)
    Box(modifier.fillMaxSize().padding(horizontal = 22.dp), contentAlignment = Alignment.CenterStart) {
        when (kind) {
            WidgetKind.Clock -> Column {
                Text(java.text.SimpleDateFormat("h:mm", java.util.Locale.US).format(java.util.Date()), color = primary, fontSize = 60.sp, fontWeight = FontWeight.Thin, lineHeight = 64.sp)
                Text(java.text.SimpleDateFormat("EEEE, MMM d", java.util.Locale.US).format(java.util.Date()), color = secondary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            }
            WidgetKind.ClockAnalog -> Row(verticalAlignment = Alignment.CenterVertically) {
                AnalogFace(primary, Modifier.size(82.dp))
                Spacer(Modifier.width(18.dp))
                Column {
                    Text(java.text.SimpleDateFormat("EEEE", java.util.Locale.US).format(java.util.Date()), color = primary, fontSize = 26.sp, fontWeight = FontWeight.Light)
                    Text(java.text.SimpleDateFormat("MMMM d", java.util.Locale.US).format(java.util.Date()), color = secondary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
            }
            WidgetKind.Weather -> Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("San Francisco", color = primary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Text("64°", color = primary, fontSize = 54.sp, fontWeight = FontWeight.Thin, lineHeight = 58.sp)
                    Text("Partly cloudy", color = secondary, fontSize = 13.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("🌤️", fontSize = 40.sp)
                    Text("H 68°  L 55°", color = secondary, fontSize = 13.sp)
                }
            }
            WidgetKind.Battery -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Bar("Battery", 0.82f, "82% ⚡", primary, secondary)
                Bar("Memory", 0.58f, "6.4/11G", primary, secondary)
                Bar("Storage", 0.46f, "118/256G", primary, secondary)
                Text("📶 Wi-Fi", color = secondary, fontSize = 12.sp)
            }
            WidgetKind.Music -> Row(Modifier.padding(start = 0.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(76.dp).clip(RoundedCornerShape(18.dp)).background(Color.White.copy(alpha = 0.25f)))
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("Midnight City", color = primary, fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("M83", color = secondary, fontSize = 13.sp)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                        Text("⏮", color = primary, fontSize = 18.sp)
                        Text("⏸", color = primary, fontSize = 18.sp)
                        Text("⏭", color = primary, fontSize = 18.sp)
                    }
                }
            }
            WidgetKind.Calendar -> Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("UP NEXT", color = secondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Text("Today 2:30 PM  Design review", color = primary, fontSize = 14.sp, maxLines = 1)
                Text("Tue 9:00 AM  Standup", color = primary, fontSize = 14.sp, maxLines = 1)
                Text("Wed 6:15 PM  Climbing", color = primary, fontSize = 14.sp, maxLines = 1)
            }
            WidgetKind.Notes -> Column(Modifier.fillMaxHeight().padding(vertical = 16.dp)) {
                Text("NOTE", color = secondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(4.dp))
                Text("Pick up the parcel\nBook train tickets\nCall mum on Sunday", color = primary, fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun Bar(label: String, value: Float, text: String, primary: Color, secondary: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = primary, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.width(64.dp))
        LinearProgressIndicator(progress = { value }, modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)), color = primary, trackColor = secondary.copy(alpha = 0.25f), gapSize = 0.dp, drawStopIndicator = {})
        Text(text, color = secondary, fontSize = 13.sp, modifier = Modifier.width(82.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End)
    }
}

@Composable
private fun AnalogFace(color: Color, modifier: Modifier) {
    androidx.compose.foundation.Canvas(modifier) {
        val c = center
        val r = size.minDimension / 2f * 0.96f
        drawCircle(color.copy(alpha = 0.55f), r, c, style = androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx()))
        for (i in 0 until 12) {
            val a = Math.toRadians(i * 30.0 - 90)
            val long = i % 3 == 0
            val from = r * (if (long) 0.88f else 0.92f)
            drawLine(color.copy(alpha = if (long) 1f else 0.6f), androidx.compose.ui.geometry.Offset(c.x + (from * Math.cos(a)).toFloat(), c.y + (from * Math.sin(a)).toFloat()),
                androidx.compose.ui.geometry.Offset(c.x + (r * 0.97f * Math.cos(a)).toFloat(), c.y + (r * 0.97f * Math.sin(a)).toFloat()), 2.dp.toPx(), androidx.compose.ui.graphics.StrokeCap.Round)
        }
        val cal = java.util.Calendar.getInstance()
        val min = cal.get(java.util.Calendar.MINUTE)
        val hour = (cal.get(java.util.Calendar.HOUR) + min / 60f)
        fun hand(angleDeg: Double, len: Float, w: Float) {
            val a = Math.toRadians(angleDeg - 90)
            drawLine(color, c, androidx.compose.ui.geometry.Offset(c.x + (len * Math.cos(a)).toFloat(), c.y + (len * Math.sin(a)).toFloat()), w, androidx.compose.ui.graphics.StrokeCap.Round)
        }
        hand(hour * 30.0, r * 0.5f, 4.dp.toPx())
        hand(min * 6.0, r * 0.75f, 3.dp.toPx())
        drawCircle(color, 3.dp.toPx(), c)
    }
}
