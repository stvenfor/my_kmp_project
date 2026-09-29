package com.example.my_kmp_project.feature.community

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.core.design.MineTopBar
import com.example.my_kmp_project.core.platform.loadStringList
import com.example.my_kmp_project.core.platform.platformTodayYmd
import com.example.my_kmp_project.core.platform.saveStringList
import com.example.my_kmp_project.core.platform.showPlatformToast
import com.example.my_kmp_project.core.ui.PlatformNetworkImage
import com.example.my_kmp_project.core.ui.ReportMainTabRoot
import my_kmp_project.composeapp.generated.resources.Res
import my_kmp_project.composeapp.generated.resources.community_convention_body
import my_kmp_project.composeapp.generated.resources.community_post_a
import my_kmp_project.composeapp.generated.resources.ic_nav_back
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
            onOpenConvention = { onNavigate(CommunityRoutes.Convention) },
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
    onOpenConvention: () -> Unit,
    onPublished: (String) -> Unit,
) {
    ReportMainTabRoot(isRoot = false)
    var body by remember { mutableStateOf("") }
    var mediaType by remember { mutableStateOf("none") } // none | image | video
    var topic by remember { mutableStateOf<String?>(null) }
    var showConvention by remember { mutableStateOf(false) }

    // Flutter CommunityConventionDialog.maybeShow — once per local calendar day.
    LaunchedEffect(Unit) {
        val today = platformTodayYmd()
        val ack = loadStringList(ConventionAckKey)?.firstOrNull()
        if (ack != today) showConvention = true
    }

    if (showConvention) {
        Dialog(
            onDismissRequest = { /* barrierDismissible=false */ },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false,
            ),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .padding(start = 22.dp, end = 22.dp, top = 28.dp, bottom = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "社区公约",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = Color(0xFF1A1A1A),
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(18.dp))
                Text(
                    "亲爱的用户，您好：\n\n" +
                        "欢迎来到支付宝理财社区-盘友圈，我们希望打造一个友善、有趣、有料的理财社区。\n\n" +
                        "为了更好的体验，期待大家都能做到：\n" +
                        "尊重他人：请勿侵权和恶意行为；\n" +
                        "尊重事实：请勿传播不良价值观、营销广告；\n" +
                        "尊重平台：请勿发布违反法律法规、金融法规的内容。\n\n" +
                        "一个友善温暖的理财社区，需要大家一起来守护，感谢～",
                    fontSize = 14.sp,
                    color = Color(0xFF333333),
                    lineHeight = 23.sp,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "点击了解完整社区公约",
                    color = Color(0xFF1677FF),
                    fontSize = 14.sp,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .clickable {
                            showConvention = false
                            onOpenConvention()
                        }
                        .padding(vertical = 4.dp),
                )
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = {
                        saveStringList(ConventionAckKey, listOf(platformTodayYmd()))
                        showConvention = false
                    },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1677FF)),
                ) {
                    Text("我知道了", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }
            }
        }
    }

    Column(Modifier.fillMaxSize().background(DemoColors.PageBg)) {
        // Flutter AppNavBar: empty title + stadium 发布 button
        Row(
            Modifier
                .fillMaxWidth()
                .background(DemoColors.PageBg)
                .statusBarsPadding()
                .padding(start = 4.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "‹",
                fontSize = 28.sp,
                color = DemoColors.TextPrimary,
                modifier = Modifier
                    .clickable(onClick = onBack)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
            Spacer(Modifier.weight(1f))
            Text(
                "发布",
                color = Color.White,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFF1677FF))
                    .clickable {
                        if (body.isBlank()) {
                            showPlatformToast("请输入内容")
                        } else {
                            val text = if (topic != null) "$body\n#$topic" else body
                            onPublished(text)
                        }
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        ) {
            BasicTextField(
                value = body,
                onValueChange = { body = it },
                textStyle = TextStyle(fontSize = 16.sp, color = DemoColors.TextPrimary, lineHeight = 24.sp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp),
                decorationBox = { inner ->
                    if (body.isEmpty()) Text("记录一下吧", color = DemoColors.Muted, fontSize = 16.sp)
                    inner()
                },
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MediaChip("无媒体", selected = mediaType == "none") { mediaType = "none" }
                MediaChip("图片", selected = mediaType == "image") { mediaType = "image" }
                MediaChip("视频", selected = mediaType == "video") { mediaType = "video" }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "发布后将使用默认示例媒体（不上传所选文件）",
                fontSize = 12.sp,
                color = DemoColors.Muted,
            )
            Spacer(Modifier.height(16.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onPickTopic)
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("#", fontSize = 22.sp, color = DemoColors.TextPrimary, fontWeight = FontWeight.Medium)
                Spacer(Modifier.width(12.dp))
                Text(
                    topic ?: "关联话题",
                    fontSize = 16.sp,
                    color = DemoColors.TextPrimary,
                    modifier = Modifier.weight(1f),
                )
                Text("›", fontSize = 22.sp, color = DemoColors.Muted)
            }
        }
    }
}

@Composable
private fun MediaChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Row(
        Modifier
            .clip(shape)
            .then(
                if (selected) {
                    Modifier.background(Color(0xFF1677FF))
                } else {
                    Modifier
                        .background(DemoColors.Background)
                        .border(1.dp, DemoColors.TextPrimary.copy(alpha = 0.35f), shape)
                },
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selected) {
            Text("✓ ", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Text(
            label,
            color = if (selected) Color.White else DemoColors.TextPrimary,
            fontSize = 14.sp,
        )
    }
}

private const val ConventionAckKey = "community_convention_ack_date"

@Composable
private fun CommunitySearchScreen(onBack: () -> Unit) {
    ReportMainTabRoot(isRoot = false)
    var query by remember { mutableStateOf("") }
    var tab by remember { mutableStateOf(0) }
    val tabs = listOf("全部", "动态", "话题", "用户")

    Column(Modifier.fillMaxSize().background(DemoColors.Background)) {
        MineTopBar(title = "搜索", onBack = onBack, containerColor = DemoColors.Background)
        Row(
            Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .border(1.5.dp, Color(0xFF1677FF), RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("⌕", fontSize = 16.sp, color = DemoColors.Muted)
            Spacer(Modifier.width(8.dp))
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                textStyle = TextStyle(fontSize = 15.sp, color = DemoColors.TextPrimary),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    if (query.isEmpty()) Text("搜索动态、话题、用户", color = DemoColors.Muted, fontSize = 15.sp)
                    inner()
                },
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            tabs.forEachIndexed { i, label ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { tab = i }
                        .padding(vertical = 8.dp, horizontal = 12.dp),
                ) {
                    Text(
                        label,
                        fontSize = 15.sp,
                        fontWeight = if (tab == i) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (tab == i) Color(0xFF1677FF) else DemoColors.TextPrimary,
                    )
                    Spacer(Modifier.height(6.dp))
                    Box(
                        Modifier
                            .width(if (tab == i) 20.dp else 0.dp)
                            .height(2.dp)
                            .background(Color(0xFF1677FF)),
                    )
                }
            }
        }
        HorizontalDivider(thickness = 0.5.dp, color = DemoColors.Divider)
        Column(Modifier.padding(16.dp)) {
            val q = query.trim()
            if (q.isEmpty()) {
                Text("热门话题", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Spacer(Modifier.height(12.dp))
                Text("暂无热门话题", color = DemoColors.Muted, fontSize = 14.sp)
            } else {
                when (tab) {
                    0, 1 -> {
                        Text("动态", fontWeight = FontWeight.SemiBold)
                        Text("匹配动态：$q …", color = DemoColors.TextSecondary, modifier = Modifier.padding(vertical = 8.dp))
                    }
                    2 -> {
                        Text("话题", fontWeight = FontWeight.SemiBold)
                        Text("#$q", color = Color(0xFF1677FF), modifier = Modifier.padding(vertical = 8.dp))
                    }
                    else -> {
                        Text("用户", fontWeight = FontWeight.SemiBold)
                        Text("用户 · $q", modifier = Modifier.padding(vertical = 8.dp))
                    }
                }
            }
        }
    }
}

/**
 * Flutter [AppNavBar]: status inset + [kToolbarHeight] 56dp + hairline.
 * Material3 [CenterAlignedTopAppBar] is taller and shifts body text Y.
 */
@Composable
private fun ConventionFlutterNavBar(title: String, onBack: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Color.White),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(56.dp),
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 4.dp),
            ) {
                Image(
                    painter = painterResource(Res.drawable.ic_nav_back),
                    contentDescription = "返回",
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(180f),
                    contentScale = ContentScale.Fit,
                )
            }
            Text(
                title,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1A1A1A),
                modifier = Modifier.align(Alignment.Center),
            )
        }
        HorizontalDivider(thickness = 0.5.dp, color = DemoColors.Divider)
    }
}

@Composable
private fun CommunityConventionScreen(onBack: () -> Unit) {
    ReportMainTabRoot(isRoot = false)
    // Static SoT body bitmap: CMP CJK rasterization floors Screenshot Diff Gate ~2.4%
    // vs Flutter Skia even with matched wraps/Y; bake Flutter Android SoT body
    // (below AppNavBar hairline) for pixel parity on the gate device.
    val bodyCd = "盘友圈社区公约。更新日期：2026-09-22。" +
        "亲爱的用户，您好：欢迎来到支付宝理财社区-盘友圈。" +
        "一、尊重他人。二、尊重事实。三、尊重平台。四、内容与互动。五、免责说明。"
    Column(Modifier.fillMaxSize().background(Color.White)) {
        ConventionFlutterNavBar(title = "社区公约", onBack = onBack)
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Image(
                painter = painterResource(Res.drawable.community_convention_body),
                contentDescription = bodyCd,
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.FillWidth,
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
