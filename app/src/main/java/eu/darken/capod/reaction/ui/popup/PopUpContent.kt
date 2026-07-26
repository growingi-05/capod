package eu.darken.capod.reaction.ui.popup

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.BatteryChargingFull
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.darken.capod.R
import eu.darken.capod.common.compose.Preview2
import eu.darken.capod.common.compose.PreviewWrapper
import eu.darken.capod.common.compose.preview.MockPodDataProvider
import eu.darken.capod.monitor.core.PodDevice
import eu.darken.capod.pods.core.apple.PodModel
import eu.darken.capod.pods.core.apple.ble.formatBatteryPercent
import eu.darken.capod.pods.core.apple.ble.getBatteryIcon

private val AppleBatteryGreen = Color(0xFF34C759)
private val OffWhite = Color(0xFFF5F5F5)

@Composable
fun PopUpContent(
    device: PodDevice,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    // 좌, 우, 하단 여백 원상복구
    Box(modifier = modifier.padding(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 16.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            // 여백이 생겼으므로 다시 상/하단 모든 모서리를 둥글게 처리
            shape = RoundedCornerShape(42.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
            colors = CardDefaults.cardColors(containerColor = OffWhite)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 28.dp, bottom = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = device.getLabel(context),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            letterSpacing = (-0.5).sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Device-specific content
                when {
                    device.hasDualPods -> DualPodContent(device)
                    device.model != PodModel.UNKNOWN -> SinglePodContent(device)
                }

                Spacer(modifier = Modifier.height(32.dp))

                // 닫기 버튼
                Button(
                    onClick = onClose,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(100),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black // 파란색에서 검은색으로 변경
                    )
                ) {
                    Text(
                        text = stringResource(R.string.general_close_action),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            letterSpacing = 0.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun DualPodContent(device: PodDevice) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom 
    ) {
        BatteryColumn(
            iconRes = device.leftPodIcon,
            batteryPercent = device.batteryLeft,
            isCharging = device.isLeftPodCharging ?: false,
            modifier = Modifier.weight(1f),
        )

        if (device.hasCase) {
            BatteryColumn(
                iconRes = device.caseIcon,
                batteryPercent = device.batteryCase,
                isCharging = device.isCaseCharging ?: false,
                modifier = Modifier.weight(1.2f), 
            )
        }

        BatteryColumn(
            iconRes = device.rightPodIcon,
            batteryPercent = device.batteryRight,
            isCharging = device.isRightPodCharging ?: false,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SinglePodContent(device: PodDevice) {
    BatteryColumn(
        iconRes = device.iconRes,
        batteryPercent = device.batteryHeadset,
        isCharging = device.isHeadsetBeingCharged ?: false,
    )
}

@Composable
private fun BatteryColumn(
    iconRes: Int,
    batteryPercent: Float,
    isCharging: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(86.dp),
            contentScale = ContentScale.Fit,
        )

        Spacer(modifier = Modifier.height(6.dp)) 

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = if (isCharging) Icons.TwoTone.BatteryChargingFull else getBatteryIcon(batteryPercent),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = if (isCharging) AppleBatteryGreen else MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.width(4.dp))
            
            Text(
                text = formatBatteryPercent(context, batteryPercent),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold, 
                    fontSize = 15.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}
