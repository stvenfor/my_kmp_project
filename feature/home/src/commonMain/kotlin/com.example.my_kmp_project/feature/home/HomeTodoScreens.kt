package com.example.my_kmp_project.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.core.design.MineTopBar
import com.example.my_kmp_project.core.platform.showPlatformToast
import com.example.my_kmp_project.core.ui.ReportMainTabRoot
import kotlinx.coroutines.launch

@Composable
internal fun PartnerPendingScreen(onBack: () -> Unit) {
    val pending = run {
        HomeTodoStore.version
        HomeTodoStore.pendingApplications()
    }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        HomeTodoStore.refresh()
    }
    ReportMainTabRoot(isRoot = false)
    Column(Modifier.fillMaxSize().background(DemoColors.PageBg)) {
        MineTopBar(title = "新伙伴待确认", onBack = onBack, containerColor = DemoColors.PageBg)
        Text(
            if (HomeTodoStore.loading) "加载中…"
            else if (pending.isEmpty()) "暂无待审申请" else "${pending.size} 位新成员",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = DemoColors.TextSecondary,
            fontSize = 13.sp,
        )
        HomeTodoStore.errorMessage?.let { message ->
            Text(
                message,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                color = DemoColors.Accent,
                fontSize = 12.sp,
            )
        }
        if (pending.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("有人申请入店后会显示在这里", color = DemoColors.TextSecondary, fontSize = 14.sp)
            }
        } else {
            pending.forEach { app ->
                Column(
                    Modifier
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White)
                        .padding(14.dp),
                ) {
                    Text(
                        "${app.displayName} · ${app.roleLabel}",
                        fontWeight = FontWeight.Medium,
                        color = DemoColors.TextPrimary,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "申请于 ${app.createdAtLabel}",
                        fontSize = 13.sp,
                        color = DemoColors.TextSecondary,
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "拒绝",
                            color = DemoColors.TextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .border(0.5.dp, DemoColors.Divider, RoundedCornerShape(8.dp))
                                .clickable {
                                    scope.launch {
                                        if (HomeTodoStore.rejectJoin(app.applicationId)) {
                                            showPlatformToast("已拒绝 ${app.displayName}")
                                        } else {
                                            showPlatformToast(HomeTodoStore.errorMessage ?: "拒绝失败")
                                        }
                                    }
                                }
                                .padding(vertical = 10.dp),
                        )
                        Text(
                            "确认入店",
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(DemoColors.Accent)
                                .clickable {
                                    scope.launch {
                                        if (HomeTodoStore.approveJoin(app.applicationId)) {
                                            showPlatformToast("已确认 ${app.displayName}")
                                        } else {
                                            showPlatformToast(HomeTodoStore.errorMessage ?: "确认失败")
                                        }
                                    }
                                }
                                .padding(vertical = 10.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun FollowUpCustomersScreen(onBack: () -> Unit) {
    val rows = run {
        HomeTodoStore.version
        HomeTodoStore.followUpCustomers()
    }
    LaunchedEffect(Unit) {
        HomeTodoStore.refresh()
    }
    TodoListScreen(
        title = "待跟进客户",
        subtitle = if (rows.isEmpty()) "暂无待跟进客户" else "共 ${rows.size} 位待跟进",
        rows = rows.map { row ->
            Triple(row.name, "下次跟进 ${row.nextFollowUp ?: "待安排"}", null)
        },
        errorMessage = HomeTodoStore.errorMessage,
        onBack = onBack,
    )
}

@Composable
internal fun AfterSalesAppointmentsScreen(onBack: () -> Unit) {
    val rows = run {
        HomeTodoStore.version
        HomeTodoStore.afterSalesAppointments()
    }
    LaunchedEffect(Unit) {
        HomeTodoStore.refresh()
    }
    TodoListScreen(
        title = "售后预约",
        subtitle = if (rows.isEmpty()) "暂无预约" else "共 ${rows.size} 条预约",
        rows = rows.map { row ->
            Triple(row.customerName, "预约日 ${row.appointmentDate ?: "待安排"}", null)
        },
        errorMessage = HomeTodoStore.errorMessage,
        onBack = onBack,
    )
}

@Composable
internal fun StoreReviewOrdersScreen(onBack: () -> Unit) {
    val rows = run {
        HomeTodoStore.version
        HomeTodoStore.reviewOrders()
    }
    LaunchedEffect(Unit) {
        HomeTodoStore.refresh()
    }
    TodoListScreen(
        title = "订单待审核",
        subtitle = if (rows.isEmpty()) "暂无待审订单" else "共 ${rows.size} 笔",
        rows = rows.map { row ->
            Triple(row.title, "店务审核单", row.statusLabel)
        },
        errorMessage = HomeTodoStore.errorMessage,
        onBack = onBack,
    )
}

@Composable
private fun TodoListScreen(
    title: String,
    subtitle: String,
    rows: List<Triple<String, String, String?>>,
    errorMessage: String?,
    onBack: () -> Unit,
) {
    ReportMainTabRoot(isRoot = false)
    Column(Modifier.fillMaxSize().background(DemoColors.PageBg)) {
        MineTopBar(title = title, onBack = onBack, containerColor = DemoColors.PageBg)
        Text(
            subtitle,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = DemoColors.TextSecondary,
            fontSize = 13.sp,
        )
        errorMessage?.let { message ->
            Text(
                message,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                color = DemoColors.Accent,
                fontSize = 12.sp,
            )
        }
        rows.forEach { row ->
            Row(
                Modifier
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(row.first, fontWeight = FontWeight.Medium, color = DemoColors.TextPrimary)
                    Spacer(Modifier.height(4.dp))
                    Text(row.second, fontSize = 13.sp, color = DemoColors.TextSecondary)
                }
                row.third?.let { badge ->
                    Text(
                        badge,
                        fontSize = 11.sp,
                        color = DemoColors.Accent,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(DemoColors.Accent.copy(alpha = 0.1f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun TodoForbiddenScreen(title: String, onBack: () -> Unit) {
    ReportMainTabRoot(isRoot = false)
    Column(Modifier.fillMaxSize().background(DemoColors.PageBg)) {
        MineTopBar(title = title, onBack = onBack, containerColor = DemoColors.PageBg)
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                Modifier
                    .padding(horizontal = 28.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .padding(horizontal = 22.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier
                        .width(64.dp)
                        .height(64.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(DemoColors.Accent.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("🔒", fontSize = 26.sp)
                }
                Spacer(Modifier.height(18.dp))
                Text(
                    "暂无查看权限",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = DemoColors.TextPrimary,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "该待办仅门店管理员可查看。\n如需处理，请联系店管开通权限，或切换有权限的账号。",
                    fontSize = 13.sp,
                    color = DemoColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                )
                Spacer(Modifier.height(22.dp))
                Text(
                    "返回",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DemoColors.Accent)
                        .clickable(onClick = onBack)
                        .padding(vertical = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun TodoEmpty(title: String, subtitle: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, fontWeight = FontWeight.SemiBold, color = DemoColors.TextPrimary)
            Spacer(Modifier.height(6.dp))
            Text(subtitle, fontSize = 13.sp, color = DemoColors.TextSecondary)
        }
    }
}

@Composable
private fun TodoCard(
    avatarLabel: String,
    title: String,
    subtitle: String,
    badge: String? = null,
    badgeColor: Color = DemoColors.Accent,
    avatarTint: Color = DemoColors.Accent.copy(alpha = 0.12f),
    avatarFg: Color = DemoColors.Accent,
    footer: (@Composable () -> Unit)? = null,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(0.5.dp, DemoColors.Divider, RoundedCornerShape(12.dp))
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .width(44.dp)
                    .height(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(avatarTint),
                contentAlignment = Alignment.Center,
            ) {
                Text(avatarLabel.take(1), color = avatarFg, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, fontSize = 13.sp, color = DemoColors.TextSecondary)
            }
            if (badge != null) {
                Text(
                    badge,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = badgeColor,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(badgeColor.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
        }
        if (footer != null) {
            Spacer(Modifier.height(12.dp))
            footer()
        }
    }
}
