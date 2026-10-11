package eu.darken.capod.main.ui.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.unit.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import eu.darken.capod.R
import eu.darken.capod.main.ui.MainActivity
import eu.darken.capod.pods.core.apple.ble.isKnownBattery
import kotlin.math.roundToInt

// ---- Ring layout tuning knobs -------------------------------------------------------------
private const val WIDE_RING_DP = 56
private const val NARROW_RING_DP = 52
private const val SHOW_PERCENT_IN_NARROW = false // iOS 2x2 shows rings only
private val WIDGET_CORNER = 24.dp
private val RING_GREEN = 0xFF34C759.toInt()
private val RING_RED = 0xFFFF3B30.toInt()
private const val LOW_BATTERY = 0.20f
// --------------------------------------------------------------------------------------------

@Composable
fun GlanceWidgetContent(
    state: WidgetRenderState,
    context: Context,
) {
    val openApp = actionStartActivity(
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
    )

    when (state) {
        is WidgetRenderState.DualPod -> GlanceDualPod(state, context, GlanceModifier.clickable(openApp))
        is WidgetRenderState.SinglePod -> GlanceSinglePod(state, GlanceModifier.clickable(openApp))
        is WidgetRenderState.Message -> GlanceMessage(state, GlanceModifier.clickable(openApp))
        is WidgetRenderState.Loading -> GlanceLoading(state, GlanceModifier.clickable(openApp))
    }
}

// ---- DualPod ------------------------------------------------------------------------------

/** One ring: phone / left pod / right pod / case. */
private data class RingSpec(
    val icon: Int,
    val fraction: Float?, // null = unknown
    val charging: Boolean,
    val inEar: Boolean,
    val estimate: String?,
)

/** Order: phone -> left pod -> right pod -> case */
private fun dualPodRings(context: Context, state: WidgetRenderState.DualPod): List<RingSpec> {
    val phone = readPhoneBattery(context)
    return listOf(
        RingSpec(R.drawable.ic_widget_phone_24, phone.fraction, phone.charging, false, null),
        RingSpec(state.leftIcon, state.leftPercent.knownOrNull(), state.leftCharging, state.leftInEar, state.leftEstimate),
        RingSpec(state.rightIcon, state.rightPercent.knownOrNull(), state.rightCharging, state.rightInEar, state.rightEstimate),
        RingSpec(state.caseIcon, state.casePercent.knownOrNull(), state.caseCharging, false, null),
    )
}

private fun Float.knownOrNull(): Float? = if (isKnownBattery(this)) this else null

@Composable
private fun GlanceDualPod(
    state: WidgetRenderState.DualPod,
    context: Context,
    clickModifier: GlanceModifier,
) {
    val iconTint = ColorFilter.tint(fixedColor(state.resolvedIconColor))

    when (state.layout) {
        // iOS medium widget: 4 rings in one row, percent below each
        BatteryLayout.WIDE -> {
            val rings = dualPodRings(context, state)
            GlanceWidgetRoot(
                bgColor = state.resolvedBgColor,
                clickModifier = clickModifier,
                horizontalPadding = 12.dp,
                verticalPadding = 12.dp,
                corner = WIDGET_CORNER,
            ) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.Top,
                ) {
                    rings.forEach { spec ->
                        RingItem(
                            context = context,
                            spec = spec,
                            ringDp = WIDE_RING_DP,
                            textColor = state.resolvedTextColor,
                            iconTint = iconTint,
                            showPercent = true,
                            modifier = GlanceModifier.defaultWeight(),
                        )
                    }
                }
                GlanceDeviceLabel(state.deviceLabel, state.theme.showDeviceLabel, state.resolvedTextColor)
            }
        }

        // iOS small widget: 2x2 rings
        BatteryLayout.NARROW -> {
            val rings = dualPodRings(context, state)
            GlanceWidgetRoot(
                bgColor = state.resolvedBgColor,
                clickModifier = clickModifier,
                horizontalPadding = 12.dp,
                verticalPadding = 12.dp,
                corner = WIDGET_CORNER,
            ) {
                rings.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = GlanceModifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        rowItems.forEach { spec ->
                            RingItem(
                                context = context,
                                spec = spec,
                                ringDp = NARROW_RING_DP,
                                textColor = state.resolvedTextColor,
                                iconTint = iconTint,
                                showPercent = SHOW_PERCENT_IN_NARROW,
                                modifier = GlanceModifier.defaultWeight(),
                            )
                        }
                    }
                }
            }
        }

        BatteryLayout.TINY_COLUMN -> {
            val textStyle = TextStyle(color = fixedColor(state.resolvedTextColor), fontSize = 12.sp)
            GlanceWidgetRoot(
                bgColor = state.resolvedBgColor,
                clickModifier = clickModifier,
                horizontalPadding = 4.dp,
                verticalPadding = 0.dp,
            ) {
                GlanceTinyPodItem(state.leftIcon, state.leftPercent, textStyle, iconTint)
                GlanceTinyPodItem(state.rightIcon, state.rightPercent, textStyle, iconTint)
                GlanceTinyPodItem(state.caseIcon, state.casePercent, textStyle, iconTint)
            }
        }
    }
}

@Composable
private fun RingItem(
    context: Context,
    spec: RingSpec,
    ringDp: Int,
    textColor: Int,
    iconTint: ColorFilter,
    showPercent: Boolean,
    modifier: GlanceModifier = GlanceModifier,
) {
    val percentText = spec.fraction?.let { "${(it * 100).roundToInt()}%" } ?: "—"
    val ring = RingBitmap.create(
        context = context,
        sizeDp = ringDp,
        fraction = spec.fraction,
        progressColor = ringColor(spec),
        trackColor = trackColor(textColor),
        charging = spec.charging,
    )
    val textStyle = TextStyle(color = fixedColor(textColor), fontSize = 13.sp)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = GlanceModifier.size(ringDp.dp),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                provider = ImageProvider(ring),
                contentDescription = percentText,
                modifier = GlanceModifier.size(ringDp.dp),
            )
            Image(
                provider = ImageProvider(spec.icon),
                contentDescription = null,
                modifier = GlanceModifier.size((ringDp * 0.45f).dp),
                colorFilter = iconTint,
            )
        }

        if (showPercent) {
            Row(
                modifier = GlanceModifier.padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = percentText, style = textStyle, maxLines = 1)
                if (spec.inEar) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_baseline_hearing_24),
                        contentDescription = null,
                        modifier = GlanceModifier.size(12.dp).padding(start = 2.dp),
                        colorFilter = iconTint,
                    )
                }
            }
            if (spec.estimate != null) {
                Text(
                    text = spec.estimate,
                    style = TextStyle(color = fixedColor(textColor), fontSize = 10.sp),
                    maxLines = 1,
                )
            }
        }
    }
}

/** Green normally, red when low and not charging (like iOS). */
private fun ringColor(spec: RingSpec): Int {
    val f = spec.fraction
    return if (!spec.charging && f != null && f <= LOW_BATTERY) RING_RED else RING_GREEN
}

/** Text color at 25% alpha as the empty part of the ring. */
private fun trackColor(textColor: Int): Int = (textColor and 0x00FFFFFF) or (0x40 shl 24)

// ---- SinglePod / Message / Loading (unchanged) --------------------------------------------

@Composable
private fun GlanceSinglePod(
    state: WidgetRenderState.SinglePod,
    clickModifier: GlanceModifier,
) {
    val iconTint = ColorFilter.tint(fixedColor(state.resolvedIconColor))

    when (state.layout) {
        BatteryLayout.WIDE, BatteryLayout.NARROW -> {
            val textStyle = TextStyle(color = fixedColor(state.resolvedTextColor), fontSize = 12.sp)
            GlanceWidgetRoot(state.resolvedBgColor, clickModifier) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(
                        provider = ImageProvider(state.batteryIcon),
                        contentDescription = null,
                        modifier = GlanceModifier.size(20.dp),
                        colorFilter = iconTint,
                    )
                    Text(
                        text = formatGlancePercent(state.percent) + estimateSuffix(state.estimate),
                        style = textStyle,
                        maxLines = 1,
                        modifier = GlanceModifier.padding(horizontal = 8.dp),
                    )
                    if (state.charging) {
                        Image(
                            provider = ImageProvider(R.drawable.ic_baseline_power_24),
                            contentDescription = null,
                            modifier = GlanceModifier.size(20.dp),
                            colorFilter = iconTint,
                        )
                    }
                    if (state.worn) {
                        Image(
                            provider = ImageProvider(R.drawable.ic_baseline_hearing_24),
                            contentDescription = null,
                            modifier = GlanceModifier.size(20.dp),
                            colorFilter = iconTint,
                        )
                    }
                }
                GlanceDeviceLabel(state.deviceLabel, state.theme.showDeviceLabel, state.resolvedTextColor)
            }
        }

        BatteryLayout.TINY_COLUMN -> {
            val textStyle = TextStyle(color = fixedColor(state.resolvedTextColor), fontSize = 12.sp)
            GlanceWidgetRoot(
                bgColor = state.resolvedBgColor,
                clickModifier = clickModifier,
                horizontalPadding = 4.dp,
                verticalPadding = 0.dp,
            ) {
                GlanceTinyPodItem(state.batteryIcon, state.percent, textStyle, iconTint)
            }
        }
    }
}

@Composable
private fun GlanceMessage(
    state: WidgetRenderState.Message,
    clickModifier: GlanceModifier,
) {
    val isCompact = state.layout == BatteryLayout.TINY_COLUMN
    val fontSize = if (isCompact) 10.sp else 12.sp
    val hPad = if (isCompact) 4.dp else 16.dp
    val vPad = if (isCompact) 0.dp else 4.dp

    GlanceWidgetRoot(
        bgColor = state.resolvedBgColor,
        clickModifier = clickModifier,
        horizontalPadding = hPad,
        verticalPadding = vPad,
    ) {
        Text(
            text = state.primaryText,
            style = TextStyle(
                color = fixedColor(state.resolvedTextColor),
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            ),
            modifier = GlanceModifier.fillMaxWidth(),
            maxLines = if (isCompact) 2 else Int.MAX_VALUE,
        )
        if (!isCompact && state.secondaryText != null) {
            Text(
                text = state.secondaryText,
                style = TextStyle(
                    color = fixedColor(state.resolvedTextColor),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                ),
                modifier = GlanceModifier.fillMaxWidth(),
                maxLines = 4,
            )
        }
    }
}

@Composable
private fun GlanceLoading(
    state: WidgetRenderState.Loading,
    clickModifier: GlanceModifier,
) {
    val isCompact = state.layout == BatteryLayout.TINY_COLUMN
    GlanceWidgetRoot(
        bgColor = state.resolvedBgColor,
        clickModifier = clickModifier,
        horizontalPadding = if (isCompact) 4.dp else 16.dp,
        verticalPadding = if (isCompact) 0.dp else 4.dp,
    ) {
        Text(
            text = "…",
            style = TextStyle(
                color = fixedColor(state.resolvedTextColor),
                fontSize = if (isCompact) 10.sp else 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            ),
            modifier = GlanceModifier.fillMaxWidth(),
        )
    }
}

// ---- Shared building blocks ---------------------------------------------------------------

@Composable
private fun GlanceWidgetRoot(
    bgColor: Int,
    clickModifier: GlanceModifier,
    horizontalPadding: Dp = 16.dp,
    verticalPadding: Dp = 4.dp,
    corner: Dp = 0.dp,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = clickModifier
            .fillMaxSize()
            .background(fixedColor(bgColor))
            .withCorner(corner)
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        content()
    }
}

@Composable
private fun GlanceTinyPodItem(
    icon: Int,
    percent: Float,
    textStyle: TextStyle,
    iconTint: ColorFilter,
    modifier: GlanceModifier = GlanceModifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            provider = ImageProvider(icon),
            contentDescription = null,
            modifier = GlanceModifier.size(14.dp),
            colorFilter = iconTint,
        )
        Text(
            text = formatGlancePercent(percent),
            style = textStyle,
            maxLines = 1,
            modifier = GlanceModifier.padding(start = 4.dp),
        )
    }
}

@Composable
private fun GlanceDeviceLabel(
    label: String?,
    visible: Boolean,
    textColor: Int,
) {
    if (visible && label != null) {
        Text(
            text = label,
            style = TextStyle(
                color = fixedColor(textColor),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            ),
        )
    }
}

private fun fixedColor(argb: Int): ColorProvider = ColorProvider(Color(argb))

private fun GlanceModifier.withCorner(corner: Dp): GlanceModifier =
    if (corner > 0.dp) this.cornerRadius(corner) else this

private fun formatGlancePercent(percent: Float): String =
    if (isKnownBattery(percent)) "${(percent * 100).roundToInt()}%" else "—"

private fun estimateSuffix(estimate: String?): String = estimate?.let { " · $it" } ?: ""
