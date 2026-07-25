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
import eu.darken.capod.main.ui.overview.cards.components.SignalIndicator
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

    Box(modifier = modifier.padding(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 16.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            // 1. 애플 특유의 둥근 모서리로 곡률 확대 (24.dp -> 36.dp)
            shape = RoundedCornerShape(36.dp),
            // 부드러운 그림자 효과
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp) // 좌우 여백 살짝 증가
                    .padding(top = 28.dp, bottom = 20.dp), // 상하 여백 증가하여 시원한 느낌 주기
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Header: label + signal indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = device.getLabel(context),
                        // 2. 타이틀을 굵게 처리하여 애플 감성 추가
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 24.sp 
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    SignalIndicator(
                        signalQuality = device.rssiQuality,
                        isLive = device.isLive,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Device-specific content
                when {
                    device.hasDualPods -> DualPodContent(device)
                    device.model != PodModel.UNKNOWN -> SinglePodContent(device)
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Close button
                Button(
                    onClick = onClose,
                    // 3. 버튼 높이를 키우고 완전한 알약(Pill) 모양으로 변경
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(100), // 완전히 둥근 모서리
                    colors = ButtonDefaults.buttonColors(
                        // 필요에 따라 iOS 스타일의 옅은 회색 배경에 파란 글씨 등으로 바꿀 수 있습니다.
                        // 여기서는 기본 테마를 유지하되 스타일만 세련되게 잡았습니다.
                    )
                ) {
                    Text(
                        text = stringResource(R.string.general_close_action),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
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
        verticalAlignment = Alignment.Bottom // 이미지 하단 정렬을 맞춰주면 더 깔끔합니다.
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
                modifier = Modifier.weight(1.2f), // 케이스가 보통 이어버드보다 크므로 공간을 살짝 더 줍니다
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
            // 4. 애플 팝업처럼 디바이스 이미지를 크게 렌렌더링 (48.dp -> 84.dp)
            modifier = Modifier.size(84.dp),
            contentScale = ContentScale.Fit,
        )

        Spacer(modifier = Modifier.height(12.dp)) // 이미지와 텍스트 사이 간격 소폭 증가

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (isCharging) {
                Icon(
                    imageVector = Icons.TwoTone.BatteryChargingFull,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp), // 아이콘 살짝 크기 키움
                    // 애플 팝업의 충전 상태는 보통 초록색 톤을 많이 씁니다 (원하시면 tint 추가 가능)
                    // tint = Color(0xFF34C759)
                )
            } else {
                Icon(
                    imageVector = getBatteryIcon(batteryPercent),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = formatBatteryPercent(context, batteryPercent),
                // 5. 배터리 텍스트를 조금 더 뚜렷하게
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}
