package eu.darken.capod.reaction.ui.popup

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
            // 배경색: iOS 스타일 완전한 흰색
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
                // 기기 이름: iOS 스타일로 크고 굵게 적용
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

                // Device-specific content (AirPods 유닛 및 본체)
                when {
                    device.hasDualPods -> DualPodContent(device)
                    device.model != PodModel.UNKNOWN -> SinglePodContent(device)
                }

                Spacer(modifier = Modifier.height(36.dp))

                // Close button: 연한 회색 배경에 검은색 글씨
                Button(
                    onClick = onClose,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF2F2F7), // iOS 시스템 연한 회색
                        contentColor = Color.Black
                    )
                ) {
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
    batteryPercent: Float, // 배터리 비율 (0.0f ~ 1.0f 범위를 가정)
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

        Spacer(modifier = Modifier.height(16.dp))

        // 원형 링 게이지 (iOS 위젯 스타일)
        Box(
            modifier = Modifier.size(28.dp),
            contentAlignment = Alignment.Center
        ) {
            // 요청하신 잔량 상태에 따른 색상 분기 로직
            val activeColor = when {
                isCharging -> Color(0xFF34C759)              // 충전 중: 무조건 초록색
                batteryPercent <= 0.20f -> Color(0xFFFF3B30) // 20% 이하: 빨간색
                batteryPercent <= 0.40f -> Color(0xFFFF9500) // 40% 이하: 주황(노랑)색
                else -> Color(0xFF34C759)                    // 그 외 (41% 이상): 초록색
            }

            Canvas(modifier = Modifier.fillMaxSize()) {
                // 1. 바탕이 되는 빈 궤도 (연한 회색)
                drawArc(
                    color = Color(0xFFE5E5EA),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                // 2. 배터리 잔량 게이지 그리기
                drawArc(
                    color = activeColor,
                    startAngle = -90f, // 12시 방향에서 시작
                    sweepAngle = 360f * batteryPercent.coerceIn(0f, 1f), 
                    useCenter = false,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // 충전 중일 때는 링 한가운데에 직접 그린 초록색 번개 아이콘 배치
            if (isCharging) {
                BoltIcon(
                    modifier = Modifier.size(12.dp),
                    color = activeColor
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 배터리 수치
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

/**
 * 라이브러리 에러 없이 깔끔한 번개 모양을 직접 그리는 커스텀 컴포저블 뷰
 */
@Composable
private fun BoltIcon(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier) {
        val path = Path().apply {
            val w = size.width
            val h = size.height
            // 번개 모양 꼭지점 좌표 그리기
            moveTo(w * 0.55f, 0f)
            lineTo(w * 0.15f, h * 0.55f)
            lineTo(w * 0.45f, h * 0.55f)
            lineTo(w * 0.35f, h)
            lineTo(w * 0.85f, h * 0.35f)
            lineTo(w * 0.5f, h * 0.35f)
            close()
        }
        drawPath(path = path, color = color)
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
