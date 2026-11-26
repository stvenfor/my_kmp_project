package com.example.my_kmp_project.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.core.design.MineTopBar
import com.example.my_kmp_project.core.platform.showPlatformToast
import com.example.my_kmp_project.core.ui.ReportMainTabRoot

@Composable
private fun CrudListScreen(
    title: String,
    items: List<HomeListRow>,
    onBack: () -> Unit,
    onItem: ((HomeListRow) -> Unit)? = null,
    onCreate: (() -> Unit)? = null,
) {
    ReportMainTabRoot(isRoot = false)
    Column(Modifier.fillMaxSize().background(DemoColors.PageBg)) {
        MineTopBar(
            title = title,
            onBack = onBack,
            containerColor = DemoColors.PageBg,
            actions = {
                if (onCreate != null) {
                    TextButton(onClick = onCreate) {
                        Text("新建", color = DemoColors.Accent)
                    }
                }
            },
        )
        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("暂无数据", color = DemoColors.TextSecondary)
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(16.dp)) {
                items(items) { row ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(DemoColors.Background)
                            .clickable { onItem?.invoke(row) }
                            .padding(14.dp)
                            .padding(bottom = 10.dp),
                    ) {
                        Text(row.title, fontWeight = FontWeight.Medium, color = DemoColors.TextPrimary)
                        Spacer(Modifier.height(4.dp))
                        Text(row.subtitle, color = DemoColors.TextSecondary, fontSize = 13.sp)
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
internal fun CrudDetailScreen(title: String, body: Any, onBack: () -> Unit) {
    val text = when (body) {
        is HomeListRow -> "${body.title}\n${body.subtitle}"
        else -> body.toString()
    }
    ReportMainTabRoot(isRoot = false)
    Column(Modifier.fillMaxSize().background(DemoColors.PageBg)) {
        MineTopBar(title = title, onBack = onBack, containerColor = DemoColors.PageBg)
        Text(
            text,
            modifier = Modifier.padding(16.dp),
            color = DemoColors.TextPrimary,
            lineHeight = 22.sp,
        )
    }
}

@Composable
private fun CrudCreateScreen(title: String, onBack: () -> Unit, onSubmit: () -> Unit) {
    var field by remember { mutableStateOf("") }
    ReportMainTabRoot(isRoot = false)
    Column(Modifier.fillMaxSize().background(DemoColors.PageBg)) {
        MineTopBar(title = title, onBack = onBack, containerColor = DemoColors.PageBg)
        Column(Modifier.padding(16.dp)) {
            OutlinedTextField(
                value = field,
                onValueChange = { field = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("标题") },
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    if (field.isBlank()) {
                        showPlatformToast("请填写标题")
                    } else {
                        onSubmit()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = DemoColors.Primary),
            ) { Text("提交") }
        }
    }
}
