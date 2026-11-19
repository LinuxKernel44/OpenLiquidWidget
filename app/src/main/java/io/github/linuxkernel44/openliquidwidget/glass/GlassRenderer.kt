package io.github.linuxkernel44.openliquidwidget.glass

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.HardwareRenderer
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RenderEffect
import android.graphics.RenderNode
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.hardware.HardwareBuffer
import android.media.ImageReader
import android.os.Build
import android.os.SystemClock
import io.github.linuxkernel44.openliquidwidget.data.GlassParams
import io.github.linuxkernel44.openliquidwidget.data.GlassQuality
import kotlin.math.sqrt

/** Result of a render: the glass bitmap plus whether it is bright (so content can pick dark text). */
class GlassResult(val bitmap: Bitmap, val isLight: Boolean)

/**
 * Renders a liquid-glass panel off-screen: a crop of the wallpaper is blurred, refracted near the rim, tinted and lit with a
 * specular edge by an AGSL shader, then read back as a bitmap that RemoteViews can show on the home screen.
 */
object GlassRenderer {
    /** Upper bound on the pixels of a rendered panel; RemoteViews bitmaps travel over Binder and have a hard size cap. */
    private const val MAX_PIXELS = 420_000

    private const val AGSL = """
uniform shader content;
uniform float2 size;
uniform float radius;
uniform float refr;
uniform float edge;
uniform float4 tint;
uniform float rim;
uniform float px;
uniform float sat;
uniform float sheenK;
uniform float depthK;
uniform float disp;

float sdRoundBox(float2 p, float2 b, float r) {
    float2 q = abs(p) - b + r;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r;
}

half4 main(float2 fc) {
    float2 hs = size * 0.5;
    float2 p = fc - hs;
    float d = sdRoundBox(p, hs, radius);
    float cover = clamp(0.5 - d, 0.0, 1.0);
    if (cover <= 0.0) return half4(0.0);

    float2 g = float2(
        sdRoundBox(p + float2(1.0, 0.0), hs, radius) - sdRoundBox(p - float2(1.0, 0.0), hs, radius),
        sdRoundBox(p + float2(0.0, 1.0), hs, radius) - sdRoundBox(p - float2(0.0, 1.0), hs, radius));
    float2 n = length(g) > 0.0001 ? normalize(g) : float2(0.0, -1.0);

    // Lens: the closer to the rim, the more the backdrop is bent. Red/green/blue bend slightly differently (dispersion).
    float depth = clamp(-d / edge, 0.0, 1.0);
    float bend = pow(1.0 - depth, 2.2);
    float2 s = n * bend * refr;
    float3 col;
    col.r = content.eval(fc - s * 1.00).r;
    col.g = content.eval(fc - s * (1.0 + 0.10 * disp)).g;
    col.b = content.eval(fc - s * (1.0 + 0.22 * disp)).b;

    // Slightly richer colour, like vibrancy.
    float l = dot(col, float3(0.299, 0.587, 0.114));
    col = clamp(mix(float3(l), col, sat), 0.0, 1.0);

    // Tint, a soft top-left sheen and a gentle darkening toward the bottom-right edge.
    col = mix(col, tint.rgb, tint.a);
    float2 uv = fc / size;
    col += 0.07 * sheenK * (1.0 - clamp(length(uv - float2(0.15, 0.0)) * 1.1, 0.0, 1.0));
    float2 light = normalize(float2(-0.55, -0.83));
    float facing = dot(n, light);
    float inner = clamp(1.0 - (-d) / (px * 14.0), 0.0, 1.0);
    col *= 1.0 - 0.16 * depthK * inner * max(-facing, 0.0);

    // Specular rim: bright where the edge faces the light (and, fainter, the opposite side).
    float band = clamp(1.0 - (-d) / (px * 1.6), 0.0, 1.0);
    float spec = band * (0.18 + 0.82 * pow(max(facing, 0.0), 1.6) + 0.35 * pow(max(-facing, 0.0), 2.0));
    col += rim * spec * 0.85;

    col = clamp(col, 0.0, 1.0);
    return half4(half3(col * cover), half(cover));
}
"""

    /** Output size in pixels for a widget of [wPx] x [hPx], plus the scale applied to it. */
    fun outputSize(wPx: Int, hPx: Int, resolution: Float = 1f): Triple<Int, Int, Float> {
        val k = minOf(1f, sqrt(MAX_PIXELS / (wPx.toFloat() * hPx))) * resolution
        return Triple((wPx * k).toInt().coerceAtLeast(8), (hPx * k).toInt().coerceAtLeast(8), k)
    }

    /**
     * Renders the glass for a widget of [wPx] x [hPx] screen pixels. [backdrop] is the screen-sized wallpaper; the crop under the
     * widget is chosen from [GlassParams.posX]/[GlassParams.posY]. Must be called off the main thread.
     */
    fun render(backdrop: Bitmap, wPx: Int, hPx: Int, density: Float, p: GlassParams, q: GlassQuality = GlassQuality()): GlassResult {
        val (w, h, k) = outputSize(wPx, hPx, q.resolution)
        val level = q.level
        val cropW = minOf(wPx, backdrop.width)
        val cropH = minOf(hPx, backdrop.height)
        val left = ((backdrop.width - cropW) * p.posX).toInt().coerceIn(0, backdrop.width - cropW)
        val top = ((backdrop.height - cropH) * p.posY).toInt().coerceIn(0, backdrop.height - cropH)
        val src = Rect(left, top, left + cropW, top + cropH)

        val unit = density * k // one dp in output pixels
        val shader = RuntimeShader(AGSL).apply {
            setFloatUniform("size", w.toFloat(), h.toFloat())
            setFloatUniform("radius", (p.cornerDp * unit).coerceAtMost(minOf(w, h) / 2f))
            setFloatUniform("refr", if (level >= 1) p.refraction * 22f * unit else 0f)
            setFloatUniform("edge", (6f + 26f * p.refraction) * unit)
            val c = p.tintColor
            setFloatUniform("tint", Color.red(c) / 255f, Color.green(c) / 255f, Color.blue(c) / 255f, p.tintAlpha)
            setFloatUniform("rim", if (level >= 2) p.rim else 0f)
            setFloatUniform("px", unit)
            setFloatUniform("sat", if (level >= 1) 1.18f else 1f)
            setFloatUniform("sheenK", if (level >= 2) 1f else 0f)
            setFloatUniform("depthK", if (level >= 3) 1f else 0f)
            setFloatUniform("disp", if (level >= 4) 1f else 0f)
        }
        val glass = RenderEffect.createRuntimeShaderEffect(shader, "content")
        val sigma = p.blur * unit * (if (level >= 4) 1.25f else 1f)
        val effect = if (sigma > 0.5f) RenderEffect.createChainEffect(glass, RenderEffect.createBlurEffect(sigma, sigma, Shader.TileMode.CLAMP)) else glass

        val reader = ImageReader.newInstance(w, h, PixelFormat.RGBA_8888, 1, HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE or HardwareBuffer.USAGE_GPU_COLOR_OUTPUT)
        val node = RenderNode("glass").apply {
            setPosition(0, 0, w, h)
            setRenderEffect(effect)
        }
        node.beginRecording().apply {
            drawBitmap(backdrop, src, Rect(0, 0, w, h), null)
            node.endRecording()
        }
        val renderer = HardwareRenderer().apply {
            setOpaque(false)
            setSurface(reader.surface)
            setContentRoot(node)
        }
        try {
            val request = renderer.createRenderRequest()
            if (Build.VERSION.SDK_INT >= 34) request.setWaitForPresent(true)
            request.syncAndDraw()
            val deadline = SystemClock.uptimeMillis() + 2000
            var image = reader.acquireNextImage()
            while (image == null && SystemClock.uptimeMillis() < deadline) {
                Thread.sleep(8)
                image = reader.acquireNextImage()
            }
            checkNotNull(image) { "glass render timed out" }
            image.use { img ->
                val hw = checkNotNull(Bitmap.wrapHardwareBuffer(checkNotNull(img.hardwareBuffer), null)) { "no bitmap from hardware buffer" }
                val bmp = hw.copy(Bitmap.Config.ARGB_8888, false)
                hw.recycle()
                return GlassResult(bmp, isLight(bmp))
            }
        } finally {
            renderer.destroy()
            reader.close()
            node.discardDisplayList()
        }
    }

    private fun isLight(bmp: Bitmap): Boolean {
        val small = Bitmap.createScaledBitmap(bmp, 6, 6, true)
        var sum = 0.0
        var n = 0
        for (y in 0 until 6) for (x in 0 until 6) {
            val c = small.getPixel(x, y)
            if (Color.alpha(c) < 128) continue
            sum += (0.299 * Color.red(c) + 0.587 * Color.green(c) + 0.114 * Color.blue(c)) / 255.0
            n++
        }
        if (small !== bmp) small.recycle()
        return n > 0 && sum / n > 0.62
    }
}
