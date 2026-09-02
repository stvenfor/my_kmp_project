package com.example.my_kmp_project.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.core.design.ImmersiveCenterTopAppBar
import com.example.my_kmp_project.feature.shell.ReportMainTabRoot

private data class DemoHomeItem(
    val id: String,
    val title: String,
    val subtitle: String,
)

@Composable
internal fun HomeScreen() {
    ReportMainTabRoot(isRoot = true)
    val items = remember {
        listOf(
            DemoHomeItem("1", "Compose Multiplatform", "共享 UI · commonMain"),
            DemoHomeItem("2", "双 Tab Demo", "首页 + 我的"),
            DemoHomeItem("3", "沉浸式顶栏", "ImmersiveCenterTopAppBar"),
            DemoHomeItem("4", "网络骨架", "NetworkFacade + DemoApiHosts"),
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DemoColors.PageBg),
    ) {
        ImmersiveCenterTopAppBar(
            title = {
                Text(
                    text = "首页 Demo",
                    fontWeight = FontWeight.Bold,
                    color = DemoColors.TextPrimary,
                    fontSize = 18.sp,
                )
            },
            containerColor = DemoColors.Toolbar,
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text(
                    text = "KMP Demo · 业务模块已清空",
                    color = DemoColors.TextSecondary,
                    fontSize = 13.sp,
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            items(items, key = { it.id }) { row ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DemoColors.Background),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = row.title,
                            color = DemoColors.TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = row.subtitle,
                            color = DemoColors.TextSecondary,
                            fontSize = 13.sp,
                        )
                    }
                }
            }
        }
    }
}
