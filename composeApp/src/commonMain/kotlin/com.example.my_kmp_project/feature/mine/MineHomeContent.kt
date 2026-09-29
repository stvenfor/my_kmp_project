package com.example.my_kmp_project.feature.mine

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.offset
import com.example.my_kmp_project.core.platform.showPlatformToast

/** Flutter `MinePage._navFadeExtent` — scroll distance to full collapsed-nav opacity. */
private val MineNavFadeExtent = 72.dp
private val MineToolbarHeight = 44.dp

@Composable
internal fun MineHomeContent(
    loggedIn: Boolean,
    displayName: String?,
    onLoginClick: () -> Unit,
    @Suppress("UNUSED_PARAMETER") onLogoutClick: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPersonalized: () -> Unit,
    onNavigate: (String) -> Unit = {},
    snackbar: (String) -> Unit,
) {
    MinePrefsStore.version
    val base = MineCatalog.profile(loggedIn, displayName)
    val selectedStoreId = MinePrefsStore.selectedStoreId
    var showSwitchStore by remember { mutableStateOf(false) }
    val storeName = if (loggedIn) MinePrefsStore.storeName() else base.storeName
    val stats = if (loggedIn) MinePrefsStore.statsForStore(selectedStoreId) else MineCatalog.guestStats
    val profile = base.copy(storeName = storeName, stats = stats)
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    val navOpacity = with(density) {
        (scrollState.value / MineNavFadeExtent.toPx()).coerceIn(0f, 1f)
    }
    val gate: (String) -> Unit = { route ->
        if (!loggedIn) onNavigate("请先登录") else onNavigate(route)
    }
    val onOpenProfile = { gate(MineRoutes.Profile) }
    val onCalendar = { gate(MineRoutes.CheckIn) }

    if (showSwitchStore && loggedIn) {
        SwitchStoreDialog(
            selectedId = selectedStoreId,
            onDismiss = { showSwitchStore = false },
            onPicked = { store ->
                MinePrefsStore.switchStore(store.id)
                showSwitchStore = false
                showPlatformToast("已切换到 ${store.name}")
            },
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MineTheme.Background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .statusBarsPadding()
                .padding(bottom = 24.dp),
        ) {
            MineTopChrome(
                loggedIn = loggedIn,
                onOpenPersonalized = onOpenPersonalized,
                onOpenSettings = onOpenSettings,
                onOpenProfile = onOpenProfile,
                onLoginClick = onLoginClick,
                onCalendar = onCalendar,
            )
            Spacer(modifier = Modifier.height(16.dp))
            ProfileCard(
                profile = profile,
                loggedIn = loggedIn,
                snackbar = snackbar,
                onLoginClick = onLoginClick,
                onSwitchStore = {
                    if (loggedIn) {
                        if (MineStoreCatalog.stores.isEmpty()) {
                            showPlatformToast("暂无可切换店铺")
                        } else {
                            showSwitchStore = true
                        }
                    } else {
                        showPlatformToast("请先登录")
                    }
                },
            )
            Spacer(modifier = Modifier.height(16.dp))
            StatsBar(stats = profile.stats)
            Spacer(modifier = Modifier.height(8.dp))
            QuickServicesSection(
                onTap = { service ->
                    val route = MineRoutes.fromLabel(service.label)
                    if (route != null) gate(route) else snackbar(service.label)
                },
            )
            FunctionSection(
                entries = MinePrefsStore.orderedFunctions(),
                onTap = { item ->
                    val route = MineRoutes.fromLabel(item.title)
                    if (route != null) gate(route) else snackbar(item.title)
                },
                onReorderHint = {
                    // Persist current catalog order as explicit prefs write (drag UI later).
                    MinePrefsStore.reorderFunctions(MinePrefsStore.functionOrderIds())
                    snackbar("功能区顺序已保存")
                },
            )
            MenuSection(
                onSettings = onOpenSettings,
                onOther = { item ->
                    val route = MineRoutes.fromLabel(item.label)
                    if (route != null) gate(route)
                    else snackbar(item.label) // Flutter toast-only rows
                },
            )
        }

        // Flutter `_MineCollapsedNavBar` — fades in over first 72px of scroll.
        if (navOpacity >= 0.05f) {
            MineCollapsedNavBar(
                loggedIn = loggedIn,
                opacity = navOpacity,
                onOpenPersonalized = onOpenPersonalized,
                onOpenSettings = onOpenSettings,
                onOpenProfile = onOpenProfile,
                onLoginClick = onLoginClick,
                onCalendar = onCalendar,
            )
        }
    }
}

@Composable
private fun MineTopChrome(
    loggedIn: Boolean,
    onOpenPersonalized: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenProfile: () -> Unit,
    onLoginClick: () -> Unit,
    onCalendar: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "我的",
            color = MineTheme.LabelPrimary,
            fontSize = MineTheme.LargeTitleSize,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.5).sp,
            lineHeight = 36.sp,
            modifier = Modifier.weight(1f),
        )
        TopIconButton(icon = MineIcons.Info, onClick = onOpenPersonalized)
        TopIconButton(icon = MineIcons.Calendar, onClick = onCalendar)
        TopIconButton(icon = MineIcons.Settings, onClick = onOpenSettings)
        // Flutter MineHeader 4th icon: profile (not logout)
        TopIconButton(
            icon = if (loggedIn) MineIcons.Person else MineIcons.Login,
            onClick = if (loggedIn) onOpenProfile else onLoginClick,
        )
    }
}

/** Flutter `_MineCollapsedNavBar` — sticky surface bar with title + header icons. */
@Composable
private fun MineCollapsedNavBar(
    loggedIn: Boolean,
    opacity: Float,
    onOpenPersonalized: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenProfile: () -> Unit,
    onLoginClick: () -> Unit,
    onCalendar: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(opacity)
            .background(MineTheme.Surface)
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(MineToolbarHeight)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "我的",
                color = MineTheme.LabelPrimary,
                fontSize = MineTheme.HeadlineSize,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
            )
            TopIconButton(icon = MineIcons.Info, onClick = onOpenPersonalized)
            TopIconButton(icon = MineIcons.Calendar, onClick = onCalendar)
            TopIconButton(icon = MineIcons.Settings, onClick = onOpenSettings)
            TopIconButton(
                icon = if (loggedIn) MineIcons.Person else MineIcons.Login,
                onClick = if (loggedIn) onOpenProfile else onLoginClick,
            )
        }
        HorizontalDivider(thickness = MineTheme.Hairline, color = MineTheme.Separator)
    }
}

@Composable
private fun TopIconButton(icon: ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MineTheme.Accent,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun ProfileCard(
    profile: MineProfileUi,
    loggedIn: Boolean,
    snackbar: (String) -> Unit,
    onLoginClick: () -> Unit,
    onSwitchStore: () -> Unit,
) {
    MineGroupedCard(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(modifier = Modifier.clickable {
                if (loggedIn) {
                    snackbar("头像") // Flutter: pickAndUpload — gap until MediaPicker
                } else {
                    onLoginClick()
                }
            }) {
                MineAvatarPlaceholder()
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = profile.displayName,
                        color = MineTheme.LabelPrimary,
                        fontSize = MineTheme.HeadlineSize,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    MineRoleBadge(label = profile.roleBadge)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(onClick = onSwitchStore),
                ) {
                    Text(
                        text = profile.storeName,
                        color = MineTheme.LabelSecondary,
                        fontSize = MineTheme.CaptionSize,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Icon(
                        imageVector = MineIcons.ChevronDown,
                        contentDescription = null,
                        tint = MineTheme.LabelSecondary,
                        modifier = Modifier.size(14.dp),
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(MineTheme.Accent.copy(alpha = 0.08f))
                            .clickable { snackbar("电子名片") }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = MineIcons.CreditCard,
                            contentDescription = null,
                            tint = MineTheme.Accent,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "电子名片",
                            color = MineTheme.Accent,
                            fontSize = 12.sp,
                        )
                    }
                    // Flutter onElectronicCardTap: toast only
                    // clickable above already snackbar("电子名片")
                    Text(
                        text = profile.maskedPhone,
                        color = MineTheme.LabelSecondary,
                        fontSize = MineTheme.CaptionSize,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsBar(stats: List<MineStat>) {
    MineGroupedCard(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp, horizontal = 8.dp),
        ) {
            stats.forEach { stat ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stat.value,
                        color = MineTheme.LabelPrimary,
                        fontSize = MineTheme.StatValueSize,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 24.sp,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stat.label,
                        color = MineTheme.LabelSecondary,
                        fontSize = MineTheme.CaptionSize,
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickServicesSection(onTap: (MineQuickService) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp),
    ) {
        Text(
            text = "常用服务",
            color = MineTheme.LabelPrimary,
            fontSize = MineTheme.SectionTitleSize,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(12.dp))
        MineGroupedCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp, horizontal = 8.dp),
            ) {
                MineCatalog.quickServices.forEach { item ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onTap(item) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(item.iconColor.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = null,
                                    tint = item.iconColor,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                            if (item.badge != null) {
                                Text(
                                    text = item.badge,
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = 8.dp, y = (-6).dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MineTheme.Danger)
                                        .padding(horizontal = 5.dp, vertical = 2.dp),
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = item.label,
                            color = MineTheme.LabelPrimary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FunctionSection(
    entries: List<MineFunctionEntry> = MineCatalog.functions,
    onTap: (MineFunctionEntry) -> Unit,
    onReorderHint: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 12.dp)
                .clickable(onClick = onReorderHint),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "个人功能",
                color = MineTheme.LabelPrimary,
                fontSize = MineTheme.SectionTitleSize,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "长按拖动顺序",
                color = MineTheme.LabelSecondary,
                fontSize = MineTheme.CaptionSize,
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            entries.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    row.forEach { item ->
                        FunctionCard(
                            item = item,
                            onTap = { onTap(item) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (row.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun FunctionCard(
    item: MineFunctionEntry,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .aspectRatio(0.92f)
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(MineTheme.RadiusMd),
                ambientColor = MineTheme.Shadow,
                spotColor = MineTheme.Shadow,
            )
            .clip(RoundedCornerShape(MineTheme.RadiusMd))
            .background(MineTheme.Surface)
            .border(MineTheme.Hairline, MineTheme.Separator, RoundedCornerShape(MineTheme.RadiusMd))
            .clickable(onClick = onTap)
            .padding(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(MineTheme.RadiusLg))
                .background(item.accentColor),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = item.iconColor,
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = item.title,
            color = MineTheme.LabelPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = item.subtitle,
            color = MineTheme.LabelSecondary,
            fontSize = 12.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (item.highlightValue != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = item.highlightValue,
                color = item.iconColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun MenuSection(
    onSettings: () -> Unit,
    onOther: (MineMenuEntry) -> Unit,
) {
    MineGroupedCard(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp),
    ) {
        MineCatalog.menuItems.forEachIndexed { index, item ->
            if (index > 0) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = 52.dp),
                    thickness = MineTheme.Hairline,
                    color = MineTheme.Separator,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (item.id == "settings") onSettings() else onOther(item)
                    }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MineTheme.Accent.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = item.label.take(1),
                        color = MineTheme.Accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = item.label,
                    color = MineTheme.LabelPrimary,
                    fontSize = MineTheme.BodySize,
                    modifier = Modifier.weight(1f),
                )
                if (item.showBadge) {
                    Box(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MineTheme.Danger),
                    )
                }
                Text(text = "›", color = MineTheme.LabelTertiary, fontSize = 18.sp)
            }
        }
    }
}
