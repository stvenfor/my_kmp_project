package com.example.my_kmp_project.feature.community

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.core.design.MineTopBar
import com.example.my_kmp_project.core.platform.showPlatformToast
import com.example.my_kmp_project.core.ui.PlatformNetworkImage
import com.example.my_kmp_project.core.ui.ReportMainTabRoot
import my_kmp_project.composeapp.generated.resources.Res
import my_kmp_project.composeapp.generated.resources.community_post_a
import org.jetbrains.compose.resources.painterResource

internal object CommunityRoutes {
    const val Publish = "/community/publish"
    const val Search = "/community/search"
    const val Convention = "/community/convention"
    const val ImagePreview = "/community/image_preview"
    const val VideoPlay = "/community/video_play"
    const val TopicSelect = "/community/topic_select"

    fun fromLabel(label: String): String? = when (label.trim()) {
        "发布动态", "社区发布" -> Publish
        "社区搜索" -> Search
        "社区公约" -> Convention
        "话题选择" -> TopicSelect
        else -> null
    }
}

/** In-memory feed prepend for publish success (Android shell reads via callback). */
internal object CommunityPublishBus {
    var lastPublishedBody: String? = null
}

@Composable
internal fun CommunityRouteHost(
    route: String,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit = {},
    previewUrls: List<String> = emptyList(),
    previewIndex: Int = 0,
    videoUrl: String? = null,
) {
    when (route) {
        CommunityRoutes.Publish -> CommunityPublishScreen(
            onBack = onBack,
            onPickTopic = { onNavigate(CommunityRoutes.TopicSelect) },
            onPublished = { body ->
                CommunityPublishBus.lastPublishedBody = body
                showPlatformToast("发布成功")
                onBack()
            },
        )
        CommunityRoutes.Search -> CommunitySearchScreen(onBack = onBack)
        CommunityRoutes.Convention -> CommunityConventionScreen(onBack = onBack)
        CommunityRoutes.TopicSelect -> CommunityTopicSelectScreen(
            onBack = onBack,
            onPick = {
                showPlatformToast("已选 #$it")
                onBack()
            },
        )
        CommunityRoutes.ImagePreview -> CommunityImagePreviewScreen(
            urls = previewUrls.ifEmpty {
                listOf("https://picsum.photos/seed/preview/800/800")
            },
            initialIndex = previewIndex,
            onBack = onBack,
        )
        CommunityRoutes.VideoPlay -> CommunityVideoPlayScreen(
            coverUrl = videoUrl ?: "https://picsum.photos/seed/video_0/640/360",
            onBack = onBack,
        )
        else -> {
            ReportMainTabRoot(isRoot = false)
            Column(Modifier.fillMaxSize().background(DemoColors.PageBg)) {
                MineTopBar(title = "社区", onBack = onBack)
                Text("未知路由 $route", modifier = Modifier.padding(16.dp))
            }
        }
    }
}

@Composable
private fun CommunityPublishScreen(
    onBack: () -> Unit,
    onPickTopic: () -> Unit,
    onPublished: (String) -> Unit,
) {
    ReportMainTabRoot(isRoot = false)
    var body by remember { mutableStateOf("") }
    var imageCount by remember { mutableStateOf(0) }
    var topic by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().background(DemoColors.PageBg)) {
        MineTopBar(
            title = "发布",
            onBack = onBack,
            actions = {
                TextButton(
                    onClick = {
                        if (body.isBlank()) {
                            showPlatformToast("请输入内容")
                        } else {
                            val text = if (topic != null) "$body\n#$topic" else body
                            onPublished(text)
                        }
                    },
                ) { Text("发布", color = DemoColors.Accent) }
            },
        )
        Column(Modifier.padding(16.dp)) {
            BasicTextField(
                value = body,
                onValueChange = { body = it },
                textStyle = TextStyle(fontSize = 16.sp, color = DemoColors.TextPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DemoColors.Background)
                    .padding(12.dp),
                decorationBox = { inner ->
                    if (body.isEmpty()) Text("分享新鲜事…", color = DemoColors.Muted)
                    inner()
                },
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "选图(${imageCount}/9)",
                    color = DemoColors.Accent,
                    modifier = Modifier.clickable {
                        if (imageCount < 9) imageCount += 1
                    },
                )
                Text(
                    topic?.let { "#$it" } ?: "选话题",
                    color = DemoColors.Accent,
                    modifier = Modifier.clickable(onClick = onPickTopic),
                )
            }
        }
    }
}

@Composable
private fun CommunitySearchScreen(onBack: () -> Unit) {
    ReportMainTabRoot(isRoot = false)
    var query by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().background(DemoColors.PageBg)) {
        MineTopBar(title = "搜索", onBack = onBack)
        BasicTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            textStyle = TextStyle(fontSize = 15.sp, color = DemoColors.TextPrimary),
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(DemoColors.Background)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            decorationBox = { inner ->
                if (query.isEmpty()) Text("搜索动态、话题、用户", color = DemoColors.Muted)
                inner()
            },
        )
        val q = query.trim()
        if (q.isEmpty()) {
            Text("输入关键词开始搜索", color = DemoColors.TextSecondary, modifier = Modifier.padding(16.dp))
        } else {
            LazyColumn(contentPadding = PaddingValues(16.dp)) {
                item {
                    Text("动态", fontWeight = FontWeight.SemiBold)
                    Text("匹配动态：$q …", color = DemoColors.TextSecondary, modifier = Modifier.padding(vertical = 8.dp))
                    HorizontalDivider()
                    Text("话题", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 12.dp))
                    Text("#$q", color = DemoColors.Accent, modifier = Modifier.padding(vertical = 8.dp))
                    HorizontalDivider()
                    Text("用户", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 12.dp))
                    Text("用户 · $q", modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun CommunityConventionScreen(onBack: () -> Unit) {
    ReportMainTabRoot(isRoot = false)
    Column(Modifier.fillMaxSize().background(DemoColors.PageBg)) {
        MineTopBar(title = "社区公约", onBack = onBack)
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Text(
                "请文明发言，禁止发布违法违规、广告骚扰、人身攻击等内容。违规将被限制发布。",
                lineHeight = 22.sp,
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(containerColor = DemoColors.Primary),
            ) { Text("我知道了") }
        }
    }
}

@Composable
private fun CommunityTopicSelectScreen(onBack: () -> Unit, onPick: (String) -> Unit) {
    val topics = listOf("Flutter开发", "户外", "生活", "汽车", "配音")
    ReportMainTabRoot(isRoot = false)
    Column(Modifier.fillMaxSize().background(DemoColors.PageBg)) {
        MineTopBar(title = "选择话题", onBack = onBack)
        LazyColumn {
            items(topics) { t ->
                Text(
                    "#$t",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPick(t) }
                        .padding(16.dp),
                    color = DemoColors.Accent,
                )
                HorizontalDivider(color = DemoColors.Divider)
            }
        }
    }
}

@Composable
private fun CommunityImagePreviewScreen(
    urls: List<String>,
    initialIndex: Int,
    onBack: () -> Unit,
) {
    var index by remember { mutableStateOf(initialIndex.coerceIn(0, urls.lastIndex.coerceAtLeast(0))) }
    ReportMainTabRoot(isRoot = false)
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        PlatformNetworkImage(
            url = urls.getOrNull(index),
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
            placeholder = Res.drawable.community_post_a,
            contentDescription = "预览",
        )
        Row(
            Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("关闭", color = Color.White, modifier = Modifier.clickable(onClick = onBack))
            Text("${index + 1}/${urls.size}", color = Color.White)
        }
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "上一张",
                color = Color.White,
                modifier = Modifier.clickable {
                    if (index > 0) index -= 1
                },
            )
            Text(
                "下一张",
                color = Color.White,
                modifier = Modifier.clickable {
                    if (index < urls.lastIndex) index += 1
                },
            )
        }
    }
}

@Composable
private fun CommunityVideoPlayScreen(coverUrl: String, onBack: () -> Unit) {
    ReportMainTabRoot(isRoot = false)
    Column(Modifier.fillMaxSize().background(Color.Black)) {
        MineTopBar(title = "视频", onBack = onBack, containerColor = Color.Black, titleColor = Color.White)
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f),
            contentAlignment = Alignment.Center,
        ) {
            PlatformNetworkImage(
                url = coverUrl,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                placeholder = Res.drawable.community_post_a,
            )
            Text("▶ 播放（mock）", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        }
        Text(
            "真播放器随 Video 模块 H.3；此处可点路径验收。",
            color = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.padding(16.dp),
        )
    }
}
