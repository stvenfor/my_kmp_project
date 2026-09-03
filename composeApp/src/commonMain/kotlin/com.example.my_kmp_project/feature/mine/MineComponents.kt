package com.example.my_kmp_project.feature.mine

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.design.DemoColors
import my_kmp_project.composeapp.generated.resources.Res
import my_kmp_project.composeapp.generated.resources.settings_personalized_settings_chevron_right
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun MineGroupedCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MineTheme.RadiusMd))
            .background(MineTheme.Surface)
            .border(0.5.dp, MineTheme.Separator, RoundedCornerShape(MineTheme.RadiusMd)),
    ) {
        content()
    }
}

@Composable
internal fun MineSectionHeader(label: String) {
    Text(
        text = label,
        color = Color(0xFF999999),
        fontSize = 13.sp,
        modifier = Modifier.padding(start = 4.dp, top = 20.dp, end = 4.dp, bottom = 8.dp),
    )
}

@Composable
internal fun MineNavRow(
    title: String,
    subtitle: String? = null,
    trailingText: String? = null,
    showChevron: Boolean = true,
    showBadge: Boolean = false,
    destructive: Boolean = false,
    onClick: (() -> Unit)?,
) {
    val titleColor = if (destructive) DemoColors.Danger else DemoColors.TextPrimary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = titleColor, fontSize = 15.sp)
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    color = DemoColors.Muted,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (showBadge) {
            Box(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(DemoColors.Danger),
            )
        }
        if (trailingText != null) {
            Text(
                text = trailingText,
                color = Color(0xFF666666),
                fontSize = 15.sp,
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        if (showChevron && onClick != null) {
            Image(
                painter = painterResource(Res.drawable.settings_personalized_settings_chevron_right),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                contentScale = ContentScale.Fit,
            )
        }
    }
}

@Composable
internal fun MineSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    showHelp: Boolean = false,
    onHelp: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                color = Color(0xFF333333),
                fontSize = 16.sp,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (showHelp && onHelp != null) {
                Text(
                    text = "?",
                    color = DemoColors.Muted,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .padding(start = 6.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onHelp)
                        .padding(4.dp),
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = DemoColors.Accent,
                checkedThumbColor = Color.White,
            ),
        )
    }
}

@Composable
internal fun MineInsetDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 16.dp),
        thickness = 0.5.dp,
        color = Color(0xFFEEEEEE),
    )
}

@Composable
internal fun MineAvatarPlaceholder() {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(MineTheme.FillSecondary)
            .border(2.dp, MineTheme.Surface, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "人",
            color = MineTheme.LabelTertiary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
internal fun MineRoleBadge(label: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MineTheme.Accent)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = MineIcons.Check,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(10.dp),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
