package com.example.my_kmp_project.feature.live

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.core.design.MineTopBar
import com.example.my_kmp_project.core.ui.ReportMainTabRoot

/**
 * Live list → room entry (mock rooms; push/realtime remain registry gaps).
 */
@Composable
internal fun LiveScreen(
    onBack: () -> Unit,
    openRoom: Boolean = false,
) {
    var selectedId by remember {
        mutableStateOf(
            if (openRoom) LiveMockData.rooms.firstOrNull()?.id else null,
        )
    }
    val selected = selectedId?.let { id -> LiveMockData.rooms.firstOrNull { it.id == id } }

    if (selected != null) {
        ReportMainTabRoot(isRoot = false)
        LiveRoomScreen(
            room = selected,
            onBack = {
                if (openRoom) onBack() else selectedId = null
            },
        )
    } else {
        ReportMainTabRoot(isRoot = false)
        LiveListContent(
            rooms = LiveMockData.rooms,
            onBack = onBack,
            onOpen = { selectedId = it },
        )
    }
}

@Composable
private fun LiveListContent(
    rooms: List<LiveRoomItem>,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DemoColors.PageBg),
    ) {
        MineTopBar(title = "直播", onBack = onBack, containerColor = DemoColors.PageBg)
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "点选进入房间入口 · 推流/实时通道见 gap registry",
                    color = DemoColors.TextSecondary,
                    fontSize = 13.sp,
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            items(rooms, key = { it.id }) { row ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpen(row.id) }
                        .padding(vertical = 12.dp),
                ) {
                    Text(
                        text = row.title,
                        color = DemoColors.TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = row.host,
                        color = DemoColors.Muted,
                        fontSize = 12.sp,
                    )
                }
                HorizontalDivider(color = DemoColors.Divider)
            }
        }
    }
}

@Composable
private fun LiveRoomScreen(
    room: LiveRoomItem,
    onBack: () -> Unit,
) {
    var joined by remember { mutableStateOf(false) }
    var signals by remember { mutableStateOf(listOf("state: idle")) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DemoColors.PageBg),
    ) {
        MineTopBar(title = "直播 ${room.id}", onBack = onBack, containerColor = DemoColors.PageBg)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .padding(horizontal = 16.dp)
                .background(DemoColors.Toolbar, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (joined) "WS: connected · paused 保持连接" else "WS: disconnected",
                color = DemoColors.TextPrimary,
                fontSize = 15.sp,
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = room.host, color = DemoColors.TextSecondary, fontSize = 14.sp)
            Text("信令（上限 30）", fontWeight = FontWeight.SemiBold, color = DemoColors.TextPrimary)
            signals.takeLast(30).forEach { line ->
                Text(line, fontSize = 12.sp, color = DemoColors.Muted)
            }
            Button(
                onClick = {
                    joined = true
                    signals = (signals + "signal: live.join payload={room=${room.id}}").takeLast(30)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = DemoColors.Primary,
                    contentColor = DemoColors.OnPrimary,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("发送 Mock 信令 live.join")
            }
            Button(
                onClick = {
                    joined = false
                    signals = signals + "state: left"
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = DemoColors.Toolbar,
                    contentColor = DemoColors.TextPrimary,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("退订 liveSignal")
            }
            Text(
                text = "Realtime SDK 未接入；本页 mock 信令列表。",
                color = DemoColors.Muted,
                fontSize = 12.sp,
            )
        }
    }
}
