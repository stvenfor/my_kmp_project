package com.example.my_kmp_project.feature.mine

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.core.design.MineTopBar
import com.example.my_kmp_project.core.network.ApiClient
import com.example.my_kmp_project.core.platform.showPlatformToast
import com.example.my_kmp_project.core.ui.ReportMainTabRoot
import com.example.my_kmp_project.core.router.MineRoutes
import com.example.my_kmp_project.feature.mine.points.MinePointsApi
import com.example.my_kmp_project.feature.mine.points.MinePointsStatus
import com.example.my_kmp_project.feature.mine.points.MinePointsTask
import kotlinx.coroutines.launch

@Composable
internal fun UnmappedMineRoute(route: String, onBack: () -> Unit) {
    ReportMainTabRoot(isRoot = false)
    Column(Modifier.fillMaxSize().background(DemoColors.PageBg)) {
        MineTopBar(title = "未映射入口", onBack = onBack)
        Text("route=$route（非宣称完成路由）", modifier = Modifier.padding(16.dp), color = DemoColors.TextPrimary)
    }
}

@Composable
internal fun PayCheckoutScreen(onBack: () -> Unit) {
    // Flutter /pay SoT is a module placeholder ("Pay 模块"), not a full checkout yet.
    ReportMainTabRoot(isRoot = false)
    Column(Modifier.fillMaxSize().background(MineTheme.Background)) {
        MineTopBar(title = "支付", onBack = onBack, containerColor = MineTheme.Surface)
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Pay 模块", color = DemoColors.TextSecondary, fontSize = 16.sp)
        }
    }
}

@Composable
internal fun CheckInShortcutScreen(onBack: () -> Unit, api: ApiClient) {
    val pointsApi = remember(api) { MinePointsApi(api) }
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<MinePointsStatus?>(null) }
    var tasks by remember { mutableStateOf<List<MinePointsTask>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    suspend fun refresh() {
        loading = true
        error = null
        try {
            status = pointsApi.fetchStatus()
            tasks = pointsApi.fetchTasks()
        } catch (e: Exception) {
            error = e.message ?: "加载积分失败"
        } finally {
            loading = false
        }
    }

    LaunchedEffect(Unit) { refresh() }

    ReportMainTabRoot(isRoot = false)
    Column(
        Modifier
            .fillMaxSize()
            .background(DemoColors.PageBg)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
    ) {
        MineTopBar(title = "签到商城", onBack = onBack, containerColor = DemoColors.PageBg)
        Column(
            Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .mineCard()
                .padding(16.dp),
        ) {
            Text("当前积分", color = DemoColors.TextSecondary, fontSize = 13.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                status?.balance?.toString() ?: if (loading) "…" else "—",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
            )
            status?.let {
                Spacer(Modifier.height(6.dp))
                Text(
                    "连续签到 ${it.streak} 天 · 今日 +${it.todayReward}",
                    color = DemoColors.TextSecondary,
                    fontSize = 13.sp,
                )
            }
            Spacer(Modifier.height(12.dp))
            val checked = status?.checkedInToday == true
            Text(
                if (checked) "今日已签到" else "今日签到",
                color = if (checked) DemoColors.TextSecondary else DemoColors.OnPrimary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(MineTheme.RadiusMd))
                    .background(if (checked) MineTheme.FillSecondary else DemoColors.Accent)
                    .clickable(enabled = !checked && !loading) {
                        scope.launch {
                            try {
                                val result = pointsApi.checkIn()
                                showPlatformToast("签到成功 +${result.points}")
                                refresh()
                            } catch (e: Exception) {
                                showPlatformToast(e.message ?: "签到失败")
                            }
                        }
                    }
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            )
            error?.let {
                Spacer(Modifier.height(10.dp))
                Text(it, color = MineTheme.Danger, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("积分任务", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, modifier = Modifier.weight(1f))
            if (loading) TextButton(onClick = { scope.launch { refresh() } }) { Text("刷新") }
        }
        Spacer(Modifier.height(8.dp))
        if (tasks.isEmpty() && !loading) {
            Text(
                "暂无可领取任务",
                color = DemoColors.TextSecondary,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        tasks.forEach { task ->
            Row(
                Modifier
                    .padding(horizontal = 16.dp, vertical = 5.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(MineTheme.RadiusMd))
                    .background(MineTheme.Surface)
                    .clickable(enabled = task.isClaimable) {
                        scope.launch {
                            try {
                                val result = pointsApi.claim(task.code)
                                showPlatformToast("领取成功 +${result.points}")
                                refresh()
                            } catch (e: Exception) {
                                showPlatformToast(e.message ?: "领取失败")
                            }
                        }
                    }
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(task.title, fontWeight = FontWeight.Medium)
                    Text("+${task.reward} 积分", color = DemoColors.Accent, fontSize = 13.sp)
                }
                Text(
                    task.progressLabel,
                    color = if (task.isClaimable) DemoColors.Accent else DemoColors.TextSecondary,
                    fontSize = 13.sp,
                )
            }
        }
    }
}
