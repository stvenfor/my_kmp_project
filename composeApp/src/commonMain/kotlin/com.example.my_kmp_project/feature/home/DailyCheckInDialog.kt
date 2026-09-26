package com.example.my_kmp_project.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.my_kmp_project.core.design.DemoColors

/**
 * Flutter `DailyCheckInDialog` / `_DailyCheckInAlert` visual + actions.
 *
 * Layout: blue gradient hero (+reward / calendar) → white body (title / streak pill /
 * CTA / 稍后再说) → translucent close circle below card.
 */
@Composable
internal fun DailyCheckInDialog(
    todayReward: Int = 10,
    streak: Int = 3,
    onCheckIn: () -> Unit,
    onDismiss: () -> Unit,
) {
    var busy by remember { mutableStateOf(false) }
    val primaryBlue = DemoColors.Accent
    val coinGold = Color(0xFFF5A623)
    val heroBrush = Brush.linearGradient(
        colors = listOf(Color(0xFF1A8CFF), primaryBlue, Color(0xFF0050C8)),
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(24.dp, RoundedCornerShape(16.dp), ambientColor = primaryBlue.copy(alpha = 0.18f))
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White),
            ) {
                // Hero
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(heroBrush)
                        .padding(start = 20.dp, top = 28.dp, end = 20.dp, bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.18f))
                            .border(1.5.dp, Color.White.copy(alpha = 0.35f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("📅", fontSize = 28.sp)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "+$todayReward",
                        color = coinGold,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp,
                        lineHeight = 40.sp,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "今日可领积分",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 13.sp,
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "每日签到",
                        color = DemoColors.TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "签到攒积分，可在签到页兑换好物",
                        color = DemoColors.TextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp,
                    )
                    if (streak > 0) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "已连续签到 $streak 天",
                            color = Color(0xFFB7791F),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(coinGold.copy(alpha = 0.12f))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(if (busy) primaryBlue.copy(alpha = 0.5f) else primaryBlue)
                            .clickable(enabled = !busy) {
                                busy = true
                                onCheckIn()
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (busy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text(
                                text = "立即签到 · +${todayReward}积分",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "稍后再说",
                        color = DemoColors.Muted,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .clickable(enabled = !busy, onClick = onDismiss)
                            .padding(vertical = 8.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.22f))
                    .clickable(enabled = !busy, onClick = onDismiss),
                contentAlignment = Alignment.Center,
            ) {
                Text("✕", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}
