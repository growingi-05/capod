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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.BatteryChargingFull
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
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

@Composable
fun PopUpContent(
    device: PodDevice,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 24.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 400.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            // 배경과 버튼 색상 스왑 (카드 배경을 완전한 흰색으로 변경)
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 28.dp, bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // 기기 이름: 글자 크기 증가 (26.sp)
                Text(
                    text = device.getLabel(context),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.8).sp,
                        fontSize = 26.sp 
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Device-specific content
                when {
                    device.hasDualPods -> DualPodContent(device)
                    device.model != PodModel.UNKNOWN -> SinglePodContent(device)
                }

                Spacer(modifier = Modifier.height(36.dp))

                // Close button: 배경을 연한 회색으로 변경하고 글자 크기 증가
                Button(
                    onClick = onClose,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF2F2F7), // 기존 카드 배경색
                        contentColor = Color.Black
                    )
                ) {
                    // 닫기 글씨: 크기 증가 (18.sp)
                    Text(
                        text = stringResource(R.string.general_close_action),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp
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
    ) {
        // Left pod
        BatteryColumn(
            iconRes = device.leftPodIcon,
            batteryPercent = device.batteryLeft,
            isCharging = device.isLeftPodCharging ?: false,
            modifier = Modifier.weight(1f),
        )

        // Case (only if device has one)
        if (device.hasCase) {
            BatteryColumn(
                iconRes = device.caseIcon,
                batteryPercent = device.batteryCase,
                isCharging = device.isCaseCharging ?: false,
                modifier = Modifier.weight(1f),
            )
        }

        // Right pod
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
            modifier = Modifier.size(72.dp),
            contentScale = ContentScale.Fit,
        )

        Spacer(modifier = Modifier.height(12.dp)) // 배터리 아이콘과의 간격

        // 기존 Row를 Column으로 변경하여 아이콘 아래에 텍스트 배치
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (isCharging) {
                Icon(
                    imageVector = Icons.TwoTone.BatteryChargingFull,
                    contentDescription = null,
                    modifier = Modifier
                        .size(24.dp) // 커진 글씨체에 맞춰 아이콘 크기도 약간 상향
                        .rotate(90f), // 오른쪽(가로)으로 90도 회전
                    tint = Color(0xFF34C759)
                )
            } else {
                Icon(
                    imageVector = getBatteryIcon(batteryPercent),
                    contentDescription = null,
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(90f), // 오른쪽(가로)으로 90도 회전
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // 배터리 수치: 얇게(Light), 크게(18.sp)
            Text(
                text = formatBatteryPercent(context, batteryPercent),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Light, 
                    fontSize = 18.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Preview2
@Composable
private fun PopUpContentDualPodPreview() = PreviewWrapper {
    PopUpContent(device = MockPodDataProvider.dualPodMonitoredMixed(), onClose = {})
}

@Preview2
@Composable
private fun PopUpContentSinglePodPreview() = PreviewWrapper {
    PopUpContent(device = MockPodDataProvider.singlePodMonitored(), onClose = {})
}
