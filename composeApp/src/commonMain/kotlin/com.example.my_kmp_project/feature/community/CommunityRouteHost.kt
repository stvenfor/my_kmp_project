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
    const val Comment = "/community/comment"

    fun fromLabel(label: String): String? = when (label.trim()) {
        "发布动态", "社区发布" -> Publish
        "社区搜索" -> Search
        "社区公约" -> Convention
        "话题选择" -> TopicSelect
        "评论", "社区评论" -> Comment
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
        CommunityRoutes.Comment -> CommunityCommentScreen(onBack = onBack)
        else -> {
            if (route.startsWith("/community/comment")) {
                CommunityCommentScreen(onBack = onBack)
            } else {
                ReportMainTabRoot(isRoot = false)
                Column(Modifier.fillMaxSize().background(DemoColors.PageBg)) {
                    MineTopBar(title = "社区", onBack = onBack)
                    Text("未知路由 $route", modifier = Modifier.padding(16.dp))
                }
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
    Column(Modifier.fillMaxSize().background(Color.White)) {
        MineTopBar(title = "社区公约", onBack = onBack, containerColor = Color.White)
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 40.dp),
        ) {
            Text(
                "盘友圈社区公约",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A1A),
                lineHeight = 28.sp,
            )
            Spacer(Modifier.height(8.dp))
            Text("更新日期：2026-09-22", fontSize = 13.sp, color = Color(0xFF888888))
            Spacer(Modifier.height(20.dp))
            Text(
                "亲爱的用户，您好：\n\n欢迎来到支付宝理财社区-盘友圈。我们希望打造一个友善、有趣、有料的理财社区，请在发布内容、评论互动前仔细阅读并遵守本公约。",
                fontSize = 15.sp,
                color = Color(0xFF333333),
                lineHeight = 24.sp,
            )
            Spacer(Modifier.height(20.dp))
            listOf(
                "一、尊重他人" to "请勿发布侵犯他人合法权益的内容，包括但不限于侮辱、诽谤、骚扰、人肉搜索、泄露他人隐私等恶意行为；请以善意、理性的方式参与讨论。",
                "二、尊重事实" to "请勿编造或传播虚假信息、不良价值观；请勿发布未经证实的投资建议或恐吓性言论；请勿发布营销广告、软文推广，以及诱导关注、导流站外交易等行为。",
                "三、尊重平台" to "请勿发布违反法律法规、监管要求或金融法规的内容，包括但不限于欺诈、违规荐股、违规金融营销、赌博、色情、暴力等；请勿干扰社区正常秩序或滥用产品功能。",
                "四、内容与互动" to "发布的图文、视频、评论应与理财社区氛围相符；禁止刷屏、恶意引战、滥用「问大家」等能力。平台有权对违规内容采取删除、限流、禁言等措施。",
                "五、免责说明" to "社区内容仅供交流参考，不构成任何投资建议。用户应独立判断并自行承担风险。因用户违规造成的损失，由用户自行承担责任。",
            ).forEach { (h, p) ->
                Text(h, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1A1A1A))
                Spacer(Modifier.height(8.dp))
                Text(p, fontSize = 15.sp, color = Color(0xFF333333), lineHeight = 24.sp)
                Spacer(Modifier.height(16.dp))
            }
            Text(
                "一个友善温暖的理财社区，需要大家一起来守护。感谢您的理解与支持～",
                fontSize = 15.sp,
                color = Color(0xFF333333),
                lineHeight = 24.sp,
            )
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

@Composable
private fun CommunityCommentScreen(onBack: () -> Unit) {
    ReportMainTabRoot(isRoot = false)
    var draft by remember { mutableStateOf("") }
    var comments by remember {
        mutableStateOf(
            listOf(
                "李四" to "说得对！周末一起去门店看看",
                "赵六" to "同感 +1，双擎确实省油",
                "客服小助手" to "欢迎到店试驾，预约通道已开放",
            ),
        )
    }
    Column(Modifier.fillMaxSize().background(DemoColors.PageBg)) {
        MineTopBar(title = "评论 ${comments.size}", onBack = onBack)
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(comments) { (name, body) ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(DemoColors.Accent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            name.take(1),
                            color = DemoColors.Accent,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                        )
                    }
                    Column {
                        Text(name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = DemoColors.TextPrimary)
                        Text(body, fontSize = 14.sp, color = DemoColors.TextSecondary)
                    }
                }
            }
        }
        HorizontalDivider(color = DemoColors.Divider)
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BasicTextField(
                value = draft,
                onValueChange = { draft = it },
                textStyle = TextStyle(fontSize = 15.sp, color = DemoColors.TextPrimary),
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(DemoColors.Background)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                decorationBox = { inner ->
                    if (draft.isEmpty()) {
                        Text("说说你的看法", color = DemoColors.TextSecondary, fontSize = 15.sp)
                    }
                    inner()
                },
            )
            TextButton(
                onClick = {
                    val text = draft.trim()
                    if (text.isEmpty()) {
                        showPlatformToast("请输入评论")
                    } else {
                        comments = listOf("我" to text) + comments
                        draft = ""
                        showPlatformToast("评论成功")
                    }
                },
            ) { Text("发送", color = DemoColors.Accent) }
        }
    }
}
