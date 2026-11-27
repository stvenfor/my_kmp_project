package com.example.my_kmp_project.feature.mine

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/** Flutter `MineStoreData` / `StoreOption`. */
data class MineStoreOption(
    val id: String,
    val name: String,
)

/**
 * Flutter `SwitchStoreDialog` — pick a store; cancel dismisses with null.
 * 门店列表由调用方从 `GET /api/v1/me/stores` 拉取后传入，不再内置样例门店。
 * Does **not** navigate to `/friend` or invite.
 */
@Composable
fun SwitchStoreDialog(
    selectedId: String,
    stores: List<MineStoreOption>,
    onDismiss: () -> Unit,
    onPicked: (MineStoreOption) -> Unit,
    loading: Boolean = false,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 40.dp),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(MineTheme.RadiusLg))
                    .background(MineTheme.Surface)
                    .padding(start = 20.dp, top = 24.dp, end = 20.dp, bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "切换店铺",
                    fontSize = MineTheme.HeadlineSize,
                    fontWeight = FontWeight.SemiBold,
                    color = MineTheme.LabelPrimary,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    when {
                        loading && stores.isEmpty() -> "正在加载门店…"
                        stores.isEmpty() -> "暂无可切换的店铺"
                        else -> "可切换多个店铺查看数据"
                    },
                    fontSize = 13.sp,
                    color = MineTheme.LabelTertiary,
                    textAlign = TextAlign.Center,
                )
                if (stores.isNotEmpty()) {
                    Spacer(Modifier.height(20.dp))
                    stores.forEachIndexed { index, store ->
                        val selected = store.id == selectedId
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .background(
                                    if (selected) MineTheme.Accent.copy(alpha = 0.08f) else MineTheme.Surface,
                                )
                                .border(
                                    width = if (selected) 1.dp else MineTheme.Hairline,
                                    color = if (selected) MineTheme.Accent else MineTheme.Separator,
                                    shape = RoundedCornerShape(24.dp),
                                )
                                .clickable {
                                    if (store.id == selectedId) onDismiss()
                                    else onPicked(store)
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                store.name,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (selected) MineTheme.Accent else MineTheme.LabelPrimary,
                            )
                        }
                        if (index != stores.lastIndex) Spacer(Modifier.height(12.dp))
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                    .background(Color.White.copy(alpha = 0.25f))
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center,
            ) {
                Text("✕", color = Color.White, fontSize = 16.sp)
            }
        }
    }
}
