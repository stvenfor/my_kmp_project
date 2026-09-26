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
internal data class MineStoreOption(
    val id: String,
    val name: String,
)

/** Flutter `MineStoreData` — default id `1` + two demo 4S stores. */
internal object MineStoreCatalog {
    const val defaultStoreId = "1"

    val stores: List<MineStoreOption> = listOf(
        MineStoreOption("1", "[4S]北京沃德龙鼎吉利"),
        MineStoreOption("2", "[4S]北京腾远吉利"),
    )

    fun resolveName(id: String): String =
        stores.firstOrNull { it.id == id }?.name ?: stores.first().name
}

/**
 * Flutter `SwitchStoreDialog` — pick a store; cancel dismisses with null.
 * Does **not** navigate to `/friend` or invite.
 */
@Composable
internal fun SwitchStoreDialog(
    selectedId: String,
    stores: List<MineStoreOption> = MineStoreCatalog.stores,
    onDismiss: () -> Unit,
    onPicked: (MineStoreOption) -> Unit,
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
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .padding(start = 20.dp, top = 24.dp, end = 20.dp, bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "切换店铺",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1A1A1A),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    if (stores.isEmpty()) "暂无可切换的店铺" else "可切换多个店铺查看数据",
                    fontSize = 13.sp,
                    color = Color(0xFF999999),
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
                                .border(
                                    width = 1.dp,
                                    color = if (selected) Color(0xFF1B82D2) else Color(0xFFE5E5E5),
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
                                color = if (selected) Color(0xFF1B82D2) else Color(0xFF1A1A1A),
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
