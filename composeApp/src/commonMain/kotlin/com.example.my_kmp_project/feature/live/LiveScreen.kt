package com.example.my_kmp_project.feature.live

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.core.design.MineTopBar
import com.example.my_kmp_project.core.ui.ReportMainTabRoot

/**
 * Flutter [LivePage] / [LiveRoomPage] mock chrome.
 * Realtime / push remain platform-gap-registry (no SDK claimed ready).
 */
@Composable
internal fun LiveScreen(
    onBack: () -> Unit,
    openRoom: Boolean = false,
) {
    var inRoom by remember { mutableStateOf(openRoom) }
    val room = LiveMockData.rooms.firstOrNull { it.id == "mock_room_001" }
        ?: LiveMockData.rooms.first()

    if (inRoom) {
        ReportMainTabRoot(isRoot = false)
        LiveRoomScreen(
            room = room,
            onBack = {
                if (openRoom) onBack() else inRoom = false
            },
        )
    } else {
        ReportMainTabRoot(isRoot = false)
        LiveListContent(
            onEnterRoom = { inRoom = true },
        )
    }
}

@Composable
private fun LiveListContent(
    onEnterRoom: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DemoColors.PageBg),
    ) {
        MineTopBar(title = "直播", onBack = null, containerColor = DemoColors.Toolbar)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "进入直播房联调 Realtime 信令",
                    color = DemoColors.TextSecondary,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onEnterRoom,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DemoColors.TextPrimary,
                        contentColor = DemoColors.OnPrimary,
                    ),
                ) {
                    Text("进入 Mock 直播房", fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun LiveRoomScreen(
    room: LiveRoomItem,
    onBack: () -> Unit,
) {
    var connectionLabel by remember { mutableStateOf("未连接") }
    var signals by remember { mutableStateOf(listOf<String>()) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DemoColors.PageBg),
    ) {
        MineTopBar(
            title = "直播 ${room.id}",
            onBack = onBack,
            containerColor = DemoColors.Toolbar,
        )
        Text(
            text = "WS: $connectionLabel · paused 保持连接",
            color = DemoColors.TextSecondary,
            fontSize = 14.sp,
            modifier = Modifier.padding(12.dp),
        )
        Button(
            onClick = {
                connectionLabel = "connected"
                signals = (
                    listOf("[signal] live.join seq=1 {roomId=${room.id}}") + signals
                    ).take(30)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = DemoColors.TextPrimary,
                contentColor = DemoColors.OnPrimary,
            ),
        ) {
            Text("发送 join 信令", fontSize = 14.sp)
        }
        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(thickness = 1.dp, color = DemoColors.Divider)
        if (signals.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("暂无信令", color = DemoColors.Muted, fontSize = 14.sp)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(signals) { line ->
                    Text(
                        text = line,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = DemoColors.TextPrimary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    )
                }
            }
        }
    }
}
