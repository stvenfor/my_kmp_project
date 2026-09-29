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
            .border(MineTheme.Hairline, MineTheme.Separator, RoundedCornerShape(MineTheme.RadiusMd)),
    ) {
        content()
    }
}

@Composable
internal fun MineSectionHeader(label: String) {
    Text(
        text = label,
        color = MineTheme.LabelTertiary,
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
    showHelp: Boolean = false,
    onHelp: (() -> Unit)? = null,
    destructive: Boolean = false,
    onClick: (() -> Unit)?,
) {
    val titleColor = if (destructive) MineTheme.Danger else MineTheme.LabelPrimary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = title, color = titleColor, fontSize = MineTheme.BodySize)
                if (showHelp && onHelp != null) {
                    Text(
                        text = "?",
                        color = MineTheme.LabelTertiary,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .padding(start = 6.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onHelp)
                            .padding(4.dp),
                    )
                }
            }
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    color = MineTheme.LabelTertiary,
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
                    .background(MineTheme.Danger),
            )
        }
        if (trailingText != null) {
            Text(
                text = trailingText,
                color = MineTheme.LabelSecondary,
                fontSize = MineTheme.BodySize,
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
    subtitle: String? = null,
    showHelp: Boolean = false,
    onHelp: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    color = MineTheme.LabelPrimary,
                    fontSize = MineTheme.BodySize,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (showHelp && onHelp != null) {
                    Text(
                        text = "?",
                        color = MineTheme.LabelTertiary,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .padding(start = 6.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onHelp)
                            .padding(4.dp),
                    )
                }
            }
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    color = MineTheme.LabelTertiary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = MineTheme.Accent,
                checkedThumbColor = Color.White,
            ),
        )
    }
}

@Composable
internal fun MineInsetDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 16.dp),
        thickness = MineTheme.Hairline,
        color = MineTheme.Separator,
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
