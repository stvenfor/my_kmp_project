package com.example.my_kmp_project.feature.media

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.core.design.MineTopBar
import com.example.my_kmp_project.core.platform.showPlatformToast
import com.example.my_kmp_project.core.ui.ReportMainTabRoot
/** Flutter `RoutePath` video graph — Android-first clickable mock. */
internal object VideoRoutes {
    const val Hub = "/video"
    const val Short = "/video/short"
    const val ShortPlay = "/video/short/play"
    const val ShortPublish = "/video/short/publish"
    const val ShortHelp = "/video/short/help"
    const val DubbingVideos = "/video/dubbing/videos"
    const val DubbingVideoDetail = "/video/dubbing/videos/detail"
    const val DubbingWorks = "/video/dubbing/works"
    const val DubbingWorkDetail = "/video/dubbing/works/detail"

    fun fromLabel(label: String): String? = when (label.trim()) {
        "视频", "视频中心", "Video" -> Hub
        "小视频", "短视频" -> Short
        "配音视频", "配音列表" -> DubbingVideos
        "配音作品" -> DubbingWorks
        else -> null
    }
}

private data class ShortItem(
    val id: String,
    val title: String,
    val duration: String,
    val status: String,
    val likes: Int,
)

private data class DubbingItem(
    val id: String,
    val title: String,
    val tags: String,
    val desc: String,
)

private object VideoMock {
    val shorts = listOf(
        ShortItem("s1", "口语跟读 · 第一课", "02:15", "已发布", 128),
        ShortItem("s2", "周末活动花絮", "00:48", "已发布", 56),
        ShortItem("s3", "学习打卡", "01:32", "上传中", 0),
        ShortItem("s4", "校园开放日", "03:05", "已发布", 210),
    )
    val dubbing = listOf(
        DubbingItem("d1", "经典台词 · 致橡树", "朗读 · 情感", "适合跟读练习"),
        DubbingItem("d2", "日常口语 · 点餐", "口语 · 场景", "场景对话配音"),
        DubbingItem("d3", "新闻播报片段", "播音 · 正式", "语速与停顿训练"),
    )
    val works = listOf(
        DubbingItem("w1", "我的作品 · 致橡树", "已完成", "时长 01:20"),
        DubbingItem("w2", "点餐练习 v2", "待审核", "时长 00:45"),
    )
}

@Composable
internal fun VideoRouteHost(
    route: String,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit = {},
) {
    when (route) {
        VideoRoutes.Hub -> VideoHubLanding(
            onBack = onBack,
            onShort = { onNavigate(VideoRoutes.Short) },
            onDubbing = { onNavigate(VideoRoutes.DubbingVideos) },
        )
        VideoRoutes.Short -> ShortVideoPage(
            onBack = onBack,
            onHelp = { onNavigate(VideoRoutes.ShortHelp) },
            onPublish = { onNavigate(VideoRoutes.ShortPublish) },
            onPlay = { onNavigate(VideoRoutes.ShortPlay) },
        )
        VideoRoutes.ShortPlay -> ShortVideoPlayPage(onBack = onBack)
        VideoRoutes.ShortPublish -> ShortVideoPublishPage(onBack = onBack) {
            showPlatformToast("已提交（mock）")
            onBack()
        }
        VideoRoutes.ShortHelp -> ShortVideoHelpPage(onBack = onBack)
        VideoRoutes.DubbingVideos -> DubbingVideoListPage(
            onBack = onBack,
            onOpen = { onNavigate(VideoRoutes.DubbingVideoDetail) },
            onWorks = { onNavigate(VideoRoutes.DubbingWorks) },
        )
        VideoRoutes.DubbingVideoDetail -> DubbingVideoDetailPage(
            onBack = onBack,
            onWorks = { onNavigate(VideoRoutes.DubbingWorks) },
        )
        VideoRoutes.DubbingWorks -> DubbingWorkListPage(
            onBack = onBack,
            onOpen = { onNavigate(VideoRoutes.DubbingWorkDetail) },
        )
        VideoRoutes.DubbingWorkDetail -> DubbingWorkDetailPage(onBack = onBack)
        else -> VideoHubLanding(
            onBack = onBack,
            onShort = { onNavigate(VideoRoutes.Short) },
            onDubbing = { onNavigate(VideoRoutes.DubbingVideos) },
        )
    }
}

@Composable
private fun VideoHubLanding(
    onBack: () -> Unit,
    onShort: () -> Unit,
    onDubbing: () -> Unit,
) {
    ReportMainTabRoot(isRoot = false)
    Column(Modifier.fillMaxSize().background(DemoColors.PageBg)) {
        MineTopBar(title = "视频", onBack = onBack)
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Video 模块入口 · 短视频 / 配音", color = DemoColors.TextSecondary, fontSize = 13.sp)
            HubCard("短视频", "列表 → 播放 → 发布 → 帮助", onShort)
            HubCard("配音", "视频列表 → 详情 → 作品", onDubbing)
        }
    }
}

@Composable
private fun HubCard(title: String, subtitle: String, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DemoColors.Background)
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = DemoColors.TextPrimary)
        Spacer(Modifier.height(4.dp))
        Text(subtitle, fontSize = 13.sp, color = DemoColors.TextSecondary)
    }
}

@Composable
private fun ShortVideoPage(
    onBack: () -> Unit,
    onHelp: () -> Unit,
    onPublish: () -> Unit,
    onPlay: () -> Unit,
) {
    ReportMainTabRoot(isRoot = false)
    Column(Modifier.fillMaxSize().background(DemoColors.PageBg)) {
        MineTopBar(
            title = "小视频",
            onBack = onBack,
            actions = {
                TextButton(onClick = onHelp) { Text("帮助", color = DemoColors.Accent) }
            },
        )
        Column(Modifier.padding(horizontal = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(56.dp).clip(CircleShape).background(DemoColors.Accent.copy(0.2f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("我", color = DemoColors.Accent, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.size(12.dp))
                Column {
                    Text("演示用户", fontWeight = FontWeight.SemiBold, color = DemoColors.TextPrimary)
                    Text("销售顾问 · 示范门店", fontSize = 12.sp, color = DemoColors.Muted)
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatCell("作品", "${VideoMock.shorts.size}")
                StatCell("获赞", "394")
                StatCell("粉丝", "128")
            }
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(DemoColors.Background)
                    .clickable(onClick = onPublish)
                    .padding(12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("+ 发布短视频", color = DemoColors.Primary, fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.height(12.dp))
            Text("我的作品", fontWeight = FontWeight.SemiBold, color = DemoColors.TextPrimary)
            Spacer(Modifier.height(8.dp))
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.padding(horizontal = 12.dp).weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(VideoMock.shorts, key = { it.id }) { item ->
                Column(
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(DemoColors.Background)
                        .clickable {
                            if (item.status == "上传中") {
                                showPlatformToast("视频上传中…")
                            } else {
                                onPlay()
                            }
                        }
                        .padding(8.dp),
                ) {
                    Box(
                        Modifier.fillMaxWidth().height(100.dp).background(DemoColors.Toolbar, RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(item.duration, fontSize = 12.sp, color = DemoColors.TextSecondary)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(item.title, fontSize = 13.sp, color = DemoColors.TextPrimary, maxLines = 2)
                    Text(item.status, fontSize = 11.sp, color = DemoColors.Muted)
                }
            }
        }
        Text(
            "没有更多了",
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            color = DemoColors.Muted,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun StatCell(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, color = DemoColors.TextPrimary)
        Text(label, fontSize = 12.sp, color = DemoColors.Muted)
    }
}

@Composable
private fun ShortVideoPlayPage(onBack: () -> Unit) {
    var playing by remember { mutableStateOf(true) }
    var liked by remember { mutableStateOf(false) }
    val player = remember { createMediaPlayer() }
    ReportMainTabRoot(isRoot = false)
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Column(Modifier.fillMaxSize()) {
            MineTopBar(
                title = "播放",
                onBack = {
                    player.stop()
                    onBack()
                },
                containerColor = Color.Transparent,
            )
            Box(
                Modifier.weight(1f).fillMaxWidth().clickable {
                    playing = !playing
                    if (playing) {
                        player.setSource(MusicSession.SAMPLE_AUDIO_URL)
                        player.play()
                    } else {
                        player.pause()
                    }
                },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (playing) "播放中 · 单击暂停" else "已暂停 · 单击继续",
                    color = Color.White,
                    fontSize = 16.sp,
                )
            }
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = { liked = !liked }) {
                    Text(if (liked) "已赞" else "赞", color = Color.White)
                }
                TextButton(onClick = { showPlatformToast("评论（mock）") }) {
                    Text("评", color = Color.White)
                }
                TextButton(onClick = { showPlatformToast("分享（mock）") }) {
                    Text("分享", color = Color.White)
                }
            }
            Text(
                "口语跟读 · 第一课  #口语",
                modifier = Modifier.padding(16.dp),
                color = Color.White,
                fontSize = 14.sp,
            )
        }
    }
}

@Composable
private fun ShortVideoPublishPage(onBack: () -> Unit, onSubmit: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var topic by remember { mutableStateOf("#口语") }
    ReportMainTabRoot(isRoot = false)
    Column(Modifier.fillMaxSize().background(DemoColors.PageBg)) {
        MineTopBar(title = "发布短视频", onBack = onBack)
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(12.dp)).background(DemoColors.Toolbar),
                contentAlignment = Alignment.Center,
            ) {
                Text("本地预览占位 · 点选视频/拍摄见 gap", color = DemoColors.TextSecondary, fontSize = 13.sp)
            }
            Text("标题", fontWeight = FontWeight.Medium, color = DemoColors.TextPrimary)
            BasicTextField(
                value = title,
                onValueChange = { if (it.length <= 40) title = it },
                textStyle = TextStyle(fontSize = 15.sp, color = DemoColors.TextPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(DemoColors.Background)
                    .padding(12.dp),
                decorationBox = { inner ->
                    if (title.isEmpty()) Text("输入标题（≤40）", color = DemoColors.Muted)
                    inner()
                },
            )
            Text("话题", fontWeight = FontWeight.Medium, color = DemoColors.TextPrimary)
            BasicTextField(
                value = topic,
                onValueChange = { topic = it },
                textStyle = TextStyle(fontSize = 15.sp, color = DemoColors.TextPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(DemoColors.Background)
                    .padding(12.dp),
            )
            Button(
                onClick = {
                    if (title.isBlank()) showPlatformToast("请填写标题") else onSubmit()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = DemoColors.Primary),
            ) {
                Text("提交", color = DemoColors.OnPrimary)
            }
        }
    }
}

@Composable
private fun ShortVideoHelpPage(onBack: () -> Unit) {
    val steps = listOf("选择或拍摄视频", "填写标题与话题", "提交后等待审核", "在「我的作品」查看")
    ReportMainTabRoot(isRoot = false)
    Column(Modifier.fillMaxSize().background(DemoColors.PageBg)) {
        MineTopBar(title = "短视频帮助", onBack = onBack)
        LazyColumn(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(steps.size) { i ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(DemoColors.Background)
                        .padding(14.dp),
                ) {
                    Text("步骤 ${i + 1}", fontWeight = FontWeight.SemiBold, color = DemoColors.Primary)
                    Spacer(Modifier.height(4.dp))
                    Text(steps[i], color = DemoColors.TextPrimary, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun DubbingVideoListPage(
    onBack: () -> Unit,
    onOpen: () -> Unit,
    onWorks: () -> Unit,
) {
    ReportMainTabRoot(isRoot = false)
    Column(Modifier.fillMaxSize().background(DemoColors.PageBg)) {
        MineTopBar(
            title = "配音视频",
            onBack = onBack,
            actions = {
                TextButton(onClick = onWorks) { Text("作品", color = DemoColors.Accent) }
            },
        )
        LazyColumn(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(VideoMock.dubbing, key = { it.id }) { item ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(DemoColors.Background)
                        .clickable(onClick = onOpen)
                        .padding(14.dp),
                ) {
                    Text(item.title, fontWeight = FontWeight.SemiBold, color = DemoColors.TextPrimary)
                    Spacer(Modifier.height(4.dp))
                    Text(item.tags, fontSize = 12.sp, color = DemoColors.Muted)
                }
            }
        }
    }
}

@Composable
private fun DubbingVideoDetailPage(onBack: () -> Unit, onWorks: () -> Unit) {
    val item = VideoMock.dubbing.first()
    ReportMainTabRoot(isRoot = false)
    Column(Modifier.fillMaxSize().background(DemoColors.PageBg).verticalScroll(rememberScrollState())) {
        MineTopBar(title = "配音详情", onBack = onBack)
        Box(
            Modifier.fillMaxWidth().height(200.dp).padding(16.dp)
                .clip(RoundedCornerShape(12.dp)).background(DemoColors.Toolbar),
            contentAlignment = Alignment.Center,
        ) {
            Text("PlayableVideoHeader（mock）", color = DemoColors.TextSecondary)
        }
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(item.title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = DemoColors.TextPrimary)
            Text(item.tags, color = DemoColors.Muted, fontSize = 13.sp)
            Text(item.desc, color = DemoColors.TextSecondary, fontSize = 14.sp)
            Text("上传者 · 官方素材库", color = DemoColors.TextSecondary, fontSize = 13.sp)
            Text("分集 · 第 1 集", color = DemoColors.TextPrimary, fontSize = 14.sp)
            TextButton(onClick = onWorks) { Text("最新作品 →", color = DemoColors.Primary) }
            Text("排行榜 · Top3 mock", color = DemoColors.Muted, fontSize = 13.sp)
        }
        Spacer(Modifier.height(24.dp))
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = { showPlatformToast("开始配音（mock）") },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = DemoColors.Primary),
            ) { Text("配音", color = DemoColors.OnPrimary) }
            Button(
                onClick = { showPlatformToast("已收藏") },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = DemoColors.Toolbar),
            ) { Text("收藏", color = DemoColors.TextPrimary) }
            Button(
                onClick = { showPlatformToast("分享") },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = DemoColors.Toolbar),
            ) { Text("分享", color = DemoColors.TextPrimary) }
        }
    }
}

@Composable
private fun DubbingWorkListPage(onBack: () -> Unit, onOpen: () -> Unit) {
    ReportMainTabRoot(isRoot = false)
    Column(Modifier.fillMaxSize().background(DemoColors.PageBg)) {
        MineTopBar(title = "配音作品", onBack = onBack)
        LazyColumn(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(VideoMock.works, key = { it.id }) { item ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(DemoColors.Background)
                        .clickable(onClick = onOpen)
                        .padding(14.dp),
                ) {
                    Text(item.title, fontWeight = FontWeight.SemiBold, color = DemoColors.TextPrimary)
                    Text(item.tags + " · " + item.desc, fontSize = 12.sp, color = DemoColors.Muted)
                }
            }
        }
    }
}

@Composable
private fun DubbingWorkDetailPage(onBack: () -> Unit) {
    var tab by remember { mutableStateOf(0) }
    ReportMainTabRoot(isRoot = false)
    Column(Modifier.fillMaxSize().background(DemoColors.PageBg)) {
        MineTopBar(title = "作品详情", onBack = onBack)
        Box(
            Modifier.fillMaxWidth().height(180.dp).padding(16.dp)
                .clip(RoundedCornerShape(12.dp)).background(DemoColors.Toolbar),
            contentAlignment = Alignment.Center,
        ) {
            Text("作品播放头（mock）", color = DemoColors.TextSecondary)
        }
        Row(Modifier.padding(horizontal = 16.dp)) {
            TextButton(onClick = { tab = 0 }) {
                Text("介绍", color = if (tab == 0) DemoColors.Primary else DemoColors.Muted)
            }
            TextButton(onClick = { tab = 1 }) {
                Text("评论", color = if (tab == 1) DemoColors.Primary else DemoColors.Muted)
            }
        }
        HorizontalDivider(color = DemoColors.Divider)
        when (tab) {
            0 -> Text(
                "作品介绍 mock · 语速适中、情感到位。",
                modifier = Modifier.padding(16.dp),
                color = DemoColors.TextPrimary,
            )
            else -> Text(
                "暂无评论（mock）",
                modifier = Modifier.padding(16.dp),
                color = DemoColors.Muted,
            )
        }
    }
}
