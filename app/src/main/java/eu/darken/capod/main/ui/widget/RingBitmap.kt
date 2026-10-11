package eu.darken.capod.main.ui.widget

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.os.BatteryManager
import kotlin.math.roundToInt

/**
 * Glance has no Canvas, so the ring gauge is drawn into a Bitmap and shown via ImageProvider(bitmap).
 * The icon in the middle is a separate Image stacked on top (see RingItem in GlanceWidgetContent.kt).
 */
object RingBitmap {

    /**
     * @param fraction 0f..1f, or null if the value is unknown (draws only the empty track)
     * @param charging draws a small bolt in a gap at the top of the ring
     */
    fun create(
        context: Context,
        sizeDp: Int,
        fraction: Float?,
        progressColor: Int,
        trackColor: Int,
        charging: Boolean,
    ): Bitmap {
        val density = context.resources.displayMetrics.density
        val px = (sizeDp * density).roundToInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val stroke = px * 0.11f
        // inset is a full stroke width so the bolt (which sticks out above the ring) isn't clipped
        val inset = stroke
        val rect = RectF(inset, inset, px - inset, px - inset)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = stroke
            strokeCap = Paint.Cap.ROUND
        }

        // Track
        paint.color = trackColor
        canvas.drawOval(rect, paint)

        // Progress (starts at 12 o'clock, clockwise)
        if (fraction != null && fraction > 0f) {
            paint.color = progressColor
            if (fraction >= 0.999f) {
                canvas.drawOval(rect, paint)
            } else {
                canvas.drawArc(rect, -90f, 360f * fraction, false, paint)
            }
        }

        // Charging bolt in a gap at the top of the ring
        if (charging) {
            val cx = px / 2f
            val cy = inset

            val clear = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            }
            canvas.drawCircle(cx, cy, stroke * 1.0f, clear)

            val h = stroke * 1.7f
            val w = h * 0.6f
            val bolt = Path().apply {
                moveTo(cx + w * 0.10f, cy - h / 2f)
                lineTo(cx - w * 0.50f, cy + h * 0.05f)
                lineTo(cx - w * 0.02f, cy + h * 0.05f)
                lineTo(cx - w * 0.10f, cy + h / 2f)
                lineTo(cx + w * 0.50f, cy - h * 0.05f)
                lineTo(cx + w * 0.02f, cy - h * 0.05f)
                close()
            }
            val boltPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = progressColor
            }
            canvas.drawPath(bolt, boltPaint)
        }

        return bitmap
    }
}

data class PhoneBattery(
    /** 0f..1f, null if unavailable */
    val fraction: Float?,
    val charging: Boolean,
)

/** Reads the phone's own battery state from the sticky ACTION_BATTERY_CHANGED broadcast. */
fun readPhoneBattery(context: Context): PhoneBattery {
    val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        ?: return PhoneBattery(null, false)

    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
    val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)

    val fraction = if (level >= 0 && scale > 0) level / scale.toFloat() else null
    val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
        status == BatteryManager.BATTERY_STATUS_FULL

    return PhoneBattery(fraction, charging)
}
