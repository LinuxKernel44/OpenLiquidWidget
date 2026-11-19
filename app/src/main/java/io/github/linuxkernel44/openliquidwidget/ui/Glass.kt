package io.github.linuxkernel44.openliquidwidget.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy

/** A liquid-glass surface (Kyant0's AndroidLiquidGlass) over [backdrop]; the app's own chrome and the gallery previews use it. */
fun Modifier.liquidGlass(backdrop: Backdrop, corner: Dp = 28.dp, blurDp: Float = 6f, tint: Color = Color.White.copy(alpha = 0.12f)): Modifier {
    val shape = RoundedCornerShape(corner)
    return drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = {
            vibrancy()
            if (blurDp > 0f) blur(blurDp.dp.toPx())
            lens(refractionHeight = 14.dp.toPx(), refractionAmount = 26.dp.toPx())
        },
        onDrawSurface = {
            drawRect(Color.Black.copy(alpha = 0.10f))
            drawRect(tint)
        },
    )
}

/** A pill-shaped glass button. */
@Composable
fun GlassButton(text: String, backdrop: Backdrop, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier.liquidGlass(backdrop, corner = 22.dp, blurDp = 4f, tint = Color.White.copy(alpha = 0.16f)).clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}
