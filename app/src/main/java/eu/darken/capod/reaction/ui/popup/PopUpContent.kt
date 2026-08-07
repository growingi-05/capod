package eu.darken.capod.reaction.ui.popup

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
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
    
    // 다크모드 여부에 따른 색상 정의
    val isDark = isSystemInDarkTheme()
    val cardBgColor = if (isDark) Color(0xFF1C1C1E) else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    val buttonBgColor = if (isDark) Color(0xFF2C2C2E) else Color(0xFFF2F2F7)

    Box(
        modifier = modifier
            .fillMaxWidth()
            // 좌우 여백 12.dp 유지 (화면 베젤과 가까운 간격)
            .padding(start = 12.dp, end = 12.dp, top = 16.dp, bottom = 24.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 400.dp)
                .fillMaxWidth()
                // 빈 공간 터치 시 닫히는 기능과 충돌하지 않도록 카드 터치 이벤트 무시
                .pointerInput(Unit) { detectTapGestures(onTap = {}) },
            shape = RoundedCornerShape(36.dp), // 곡률 대폭 증가 (기존 24.dp -> 36.dp)
            colors = CardDefaults.cardColors(containerColor = cardBgColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 44.dp), // 콘텐츠와 하단 배경 사이의 여백 증가 (기존 36.dp -> 44.dp)
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // 상단 헤더 영역: 텍스트와 X 버튼을 동일 선상에 정렬
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        // 텍스트/버튼과 배경 상단/좌우 사이의 여백을 넉넉하게 증가 (기존 24.dp -> 32.dp)
                        .padding(top = 32.dp, start = 32.dp, end = 32.dp)
                ) {
                    // 기기 이름 (중앙 정렬)
                    Text(
                        text = device.getLabel(context),
                        color = textColor,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = (-0.8).sp,
                            fontSize = 30.sp // 글자 크기 소폭 증가 (기존 28.sp -> 30.sp)
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 36.dp) // X 버튼과 겹치지 않도록 방어 여백
                    )

                    // 닫기 X 버튼 (우측 중앙 정렬, 회색 원형 배경)
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(color = buttonBgColor)
                            .clickable(onClick = onClose),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.general_close_action),
                            tint = textColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(40.dp)) // 상단 이름과 에어팟 아이콘 사이 간격을 늘려 팝업 높이 확장

                // Device-specific content (AirPods 유닛 및 본체)
                when {
                    device.hasDualPods -> DualPodContent(device, isDark)
                    device.model != PodModel.UNKNOWN -> SinglePodContent(device, isDark)
                }
            }
        }
    }
}

@Composable
private fun DualPodContent(device: PodDevice, isDark: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        // 좌우 유닛과 케이스 사이의 간격을 16.dp로 좁히고 중앙으로 타이트하게 모음
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
    ) {
        // Left pod (weight Modifier 제거)
        BatteryColumn(
            iconRes = device.leftPodIcon,
            batteryPercent = device.batteryLeft,
            isCharging = device.isLeftPodCharging ?: false,
            isDark = isDark,
        )

        // Case (only if device has one)
        if (device.hasCase) {
            BatteryColumn(
                iconRes = device.caseIcon,
                batteryPercent = device.batteryCase,
                isCharging = device.isCaseCharging ?: false,
                isDark = isDark,
            )
        }

        // Right pod (weight Modifier 제거)
        BatteryColumn(
            iconRes = device.rightPodIcon,
            batteryPercent = device.batteryRight,
            isCharging = device.isRightPodCharging ?: false,
            isDark = isDark,
        )
    }
}

@Composable
private fun SinglePodContent(device: PodDevice, isDark: Boolean) {
    BatteryColumn(
        iconRes = device.iconRes,
        batteryPercent = device.batteryHeadset,
        isCharging = device.isHeadsetBeingCharged ?: false,
        isDark = isDark
    )
}

@Composable
private fun BatteryColumn(
    iconRes: Int,
    batteryPercent: Float, 
    isCharging: Boolean = false,
    isDark: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val textColor = if (isDark) Color.White else Color.Black
    val trackColor = if (isDark) Color(0xFF3A3A3C) else Color(0xFFE5E5EA)

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
            // 잔량 상태에 따른 색상 분기 로직 (41% 이상은 무조건 초록색으로 고정)
            val activeColor = when {
                isCharging -> Color(0xFF34C759)              
                batteryPercent <= 0.20f -> Color(0xFFFF3B30) 
                batteryPercent <= 0.40f -> Color(0xFFFF9500) 
                else -> Color(0xFF34C759) 
            }

            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 3.dp.toPx()
                val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)

                // 1. 바탕이 되는 빈 궤도 (다크모드 대응 회색)
                drawArc(
                    color = trackColor,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = stroke
                )

                // 2. 배터리 잔량 게이지 그리기
                drawArc(
                    color = activeColor,
                    startAngle = -90f, 
                    sweepAngle = 360f * batteryPercent.coerceIn(0f, 1f), 
                    useCenter = false,
                    style = stroke
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
        
        // 배터리 수치 텍스트 (다크모드 색상 반영)
        Text(
            text = formatBatteryPercent(context, batteryPercent),
            color = textColor,
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
