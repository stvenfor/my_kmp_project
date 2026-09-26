package com.example.my_kmp_project.feature.media

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.core.design.MineTopBar
import com.example.my_kmp_project.core.platform.showPlatformToast
import com.example.my_kmp_project.core.ui.ReportMainTabRoot
import my_kmp_project.composeapp.generated.resources.Res
import my_kmp_project.composeapp.generated.resources.community_avatar
import my_kmp_project.composeapp.generated.resources.home_all_services_dubbing_home
import my_kmp_project.composeapp.generated.resources.home_all_services_small_video
import my_kmp_project.composeapp.generated.resources.home_dubbing_home_cover_01
import my_kmp_project.composeapp.generated.resources.home_dubbing_home_cover_02
import my_kmp_project.composeapp.generated.resources.home_dubbing_home_cover_03
import my_kmp_project.composeapp.generated.resources.home_dubbing_home_cover_04
import my_kmp_project.composeapp.generated.resources.home_dubbing_home_cover_05
import my_kmp_project.composeapp.generated.resources.home_dubbing_home_cover_06
import my_kmp_project.composeapp.generated.resources.home_dubbing_home_cover_07
import my_kmp_project.composeapp.generated.resources.home_dubbing_home_cover_08
import my_kmp_project.composeapp.generated.resources.home_dubbing_home_cover_09
import my_kmp_project.composeapp.generated.resources.home_dubbing_home_cover_10
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

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
        "视频", "视频中心", "Video", "视频号", "素材库", "视频列表" -> Hub
        "小视频", "短视频" -> Short
        "配音视频", "配音列表" -> DubbingVideos
        "配音作品", "作品列表" -> DubbingWorks
        else -> null
    }
}

private data class ShortItem(
    val id: String,
    val title: String,
    val duration: String,
    val status: String,
    val likes: Int,
    val cover: DrawableResource,
    val aspectRatio: Float = 1.35f,
)

private data class DubbingItem(
    val id: String,
    val title: String,
    val tags: String,
    val desc: String,
    val cover: DrawableResource = Res.drawable.home_dubbing_home_cover_05,
)

private object VideoMock {
    val shorts = listOf(
        ShortItem("s1", "口语跟读 · 第一课", "02:15", "已发布", 128, Res.drawable.home_dubbing_home_cover_01, 1.45f),
        ShortItem("s2", "周末活动花絮", "00:48", "已发布", 56, Res.drawable.home_dubbing_home_cover_02, 1.2f),
        ShortItem("s3", "学习打卡", "01:32", "上传中", 0, Res.drawable.home_dubbing_home_cover_03, 1.5f),
        ShortItem("s4", "校园开放日", "03:05", "已发布", 210, Res.drawable.home_dubbing_home_cover_04, 1.3f),
        ShortItem("s5", "社团晚会精选", "01:08", "审核中", 42, Res.drawable.home_dubbing_home_cover_06, 1.4f),
    )
    val dubbing = listOf(
        DubbingItem("d1", "经典台词 · 致橡树", "朗读 · 情感", "适合跟读练习", Res.drawable.home_dubbing_home_cover_05),
        DubbingItem("d2", "日常口语 · 点餐", "口语 · 场景", "场景对话配音", Res.drawable.home_dubbing_home_cover_07),
        DubbingItem("d3", "新闻播报片段", "播音 · 正式", "语速与停顿训练", Res.drawable.home_dubbing_home_cover_08),
    )
    val works = listOf(
        DubbingItem("w1", "我的作品 · 致橡树", "已完成", "时长 01:20", Res.drawable.home_dubbing_home_cover_09),
        DubbingItem("w2", "点餐练习 v2", "待审核", "时长 00:45", Res.drawable.home_dubbing_home_cover_10),
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
        MineTopBar(title = "视频", onBack = onBack, containerColor = Color.White)
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            HubMediaCard(
                title = "小视频",
                subtitle = "作品墙 · 发布 · 全屏播放",
                cover = Res.drawable.home_all_services_small_video,
                onClick = onShort,
            )
            HubMediaCard(
                title = "配音",
                subtitle = "素材库 · 作品 · 排行",
                cover = Res.drawable.home_all_services_dubbing_home,
                onClick = onDubbing,
            )
        }
    }
}

@Composable
private fun HubMediaCard(
    title: String,
    subtitle: String,
    cover: DrawableResource,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(cover),
            contentDescription = title,
            modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop,
        )
        Spacer(Modifier.size(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = DemoColors.TextPrimary)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, fontSize = 13.sp, color = DemoColors.Muted)
        }
        Text("›", fontSize = 22.sp, color = DemoColors.Muted)
    }
}

@Composable
private fun ShortVideoPage(
    onBack: () -> Unit,
    onHelp: () -> Unit,
    onPublish: () -> Unit,
    onPlay: () -> Unit,
) {
    // Flutter ShortVideoPage: white AppNavBar + gradient under profile +
    // section「我发布的小视频」+ masonry grid with publish tile as first cell.
    ReportMainTabRoot(isRoot = false)
    Column(Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {
        MineTopBar(
            title = "小视频",
            onBack = onBack,
            containerColor = Color.White,
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(span = { GridItemSpan(2) }) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFFDCEEF9), Color(0xFFF5F5F5)),
                            ),
                        )
                        .padding(bottom = 4.dp),
                ) {
                    Spacer(Modifier.height(4.dp))
                    ShortVideoProfileCard()
                }
            }
            item(span = { GridItemSpan(2) }) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "我发布的小视频",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF171717),
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "如何拍摄小视频",
                        fontSize = 13.sp,
                        color = DemoColors.Primary,
                        modifier = Modifier.clickable(onClick = onHelp),
                    )
                }
            }
            item {
                ShortVideoPublishTile(onClick = onPublish)
            }
            items(VideoMock.shorts, key = { it.id }) { item ->
                ShortVideoCoverTile(item = item, onPlay = onPlay)
            }
            item(span = { GridItemSpan(2) }) {
                Text(
                    "没有更多了",
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    textAlign = TextAlign.Center,
                    color = DemoColors.Muted,
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
private fun ShortVideoProfileCard() {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 0.dp, vertical = 12.dp)
            .shadow(8.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                // Flutter displayProfile defaults / live SoT
                Text("qa_user", fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = DemoColors.TextPrimary)
                Spacer(Modifier.height(4.dp))
                Text("销售顾问 · [4S]北京沃德龙鼎吉利", fontSize = 13.sp, color = DemoColors.Muted)
            }
            Image(
                painter = painterResource(Res.drawable.community_avatar),
                contentDescription = "头像",
                modifier = Modifier.size(48.dp).clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            StatCell("作品", "${VideoMock.shorts.size}")
            StatCell("获赞", "394")
            StatCell("粉丝", "128")
        }
    }
}

@Composable
private fun ShortVideoPublishTile(onClick: () -> Unit) {
    // Flutter ShortVideoPublishTile — half-grid cell, dashed blue border
    Column(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1f / 1.35f)
            .border(BorderStroke(1.5.dp, Color(0xFF91D5FF)), RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier.size(44.dp).clip(CircleShape).background(DemoColors.Primary),
            contentAlignment = Alignment.Center,
        ) {
            Text("+", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(10.dp))
        Text("发布小视频", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = DemoColors.Primary)
        Spacer(Modifier.height(6.dp))
        Text(
            "拍视频，增加人气（长按已发布小视频可删除）",
            fontSize = 11.sp,
            color = DemoColors.Muted,
            textAlign = TextAlign.Center,
            lineHeight = 15.sp,
        )
    }
}

@Composable
private fun ShortVideoCoverTile(item: ShortItem, onPlay: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1f / item.aspectRatio)
            .clip(RoundedCornerShape(8.dp))
            .clickable {
                when (item.status) {
                    "上传中" -> showPlatformToast("视频上传中，请稍后再试")
                    else -> onPlay()
                }
            },
    ) {
        Image(
            painter = painterResource(item.cover),
            contentDescription = item.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        if (item.status == "上传中") {
            Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.55f)))
        }
        if (item.status == "审核中") {
            Text(
                "审核中",
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                color = Color.White,
                fontSize = 11.sp,
            )
        }
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color(0xCC000000)),
                    ),
                )
                .padding(start = 8.dp, end = 8.dp, top = 28.dp, bottom = 8.dp),
        ) {
            Column {
                Text(
                    item.title,
                    color = Color.White,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.height(2.dp))
                Text("▶ ${item.likes} · ${item.duration}", color = Color.White.copy(0.85f), fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun StatCell(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = DemoColors.TextPrimary)
        Spacer(Modifier.height(2.dp))
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
                Text("本地预览 · 选视频/拍摄见 platform-gap", color = DemoColors.TextSecondary, fontSize = 13.sp)
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
        LazyColumn(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(VideoMock.dubbing, key = { it.id }) { item ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .clickable(onClick = onOpen)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(
                        painter = painterResource(item.cover),
                        contentDescription = item.title,
                        modifier = Modifier.size(72.dp).clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop,
                    )
                    Spacer(Modifier.size(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(item.title, fontWeight = FontWeight.SemiBold, color = DemoColors.TextPrimary, fontSize = 15.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(item.tags, fontSize = 12.sp, color = DemoColors.Primary)
                        Spacer(Modifier.height(4.dp))
                        Text(item.desc, fontSize = 12.sp, color = DemoColors.Muted)
                    }
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
        Image(
            painter = painterResource(item.cover),
            contentDescription = item.title,
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .padding(16.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop,
        )
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
        LazyColumn(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(VideoMock.works, key = { it.id }) { item ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .clickable(onClick = onOpen)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(
                        painter = painterResource(item.cover),
                        contentDescription = item.title,
                        modifier = Modifier.size(64.dp).clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop,
                    )
                    Spacer(Modifier.size(12.dp))
                    Column {
                        Text(item.title, fontWeight = FontWeight.SemiBold, color = DemoColors.TextPrimary)
                        Text(item.tags + " · " + item.desc, fontSize = 12.sp, color = DemoColors.Muted)
                    }
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
        Image(
            painter = painterResource(VideoMock.works.first().cover),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(16.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop,
        )
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
