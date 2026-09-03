package com.example.my_kmp_project.feature.chat

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.core.design.MineTopBar
import com.example.my_kmp_project.feature.shell.ReportMainTabRoot

/**
 * Authenticated Chat tab root (soft gate is owned by shell/auth — this screen assumes
 * the caller only shows it after login).
 */
@Composable
internal fun ChatScreen(
    engine: ImEngine = remember { MockImEngine() },
) {
    var selectedConversationId by remember { mutableStateOf<String?>(null) }
    var messageEpoch by remember { mutableStateOf(0) }
    val selected = selectedConversationId?.let { id ->
        engine.conversations().firstOrNull { it.id == id }
    }

    if (selected != null) {
        ReportMainTabRoot(isRoot = false)
        val messages = remember(selected.id, messageEpoch) { engine.messages(selected.id) }
        ChatDetailScreen(
            conversation = selected,
            messages = messages,
            onBack = { selectedConversationId = null },
            onSend = { text ->
                engine.sendText(selected.id, text)
                messageEpoch += 1
            },
        )
    } else {
        ReportMainTabRoot(isRoot = true)
        ChatListContent(
            conversations = engine.conversations(),
            onOpen = { selectedConversationId = it },
        )
    }
}

@Composable
private fun ChatListContent(
    conversations: List<ImConversation>,
    onOpen: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DemoColors.PageBg)
            .statusBarsPadding(),
    ) {
        Text(
            text = "聊天",
            color = DemoColors.TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            letterSpacing = (-0.5).sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        if (conversations.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "暂无会话",
                    color = DemoColors.TextSecondary,
                    fontSize = 15.sp,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DemoColors.Background),
            ) {
                items(conversations, key = { it.id }) { row ->
                    ConversationRow(
                        conversation = row,
                        onClick = { onOpen(row.id) },
                    )
                    HorizontalDivider(
                        color = DemoColors.Divider,
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(start = 72.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ConversationRow(
    conversation: ImConversation,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(DemoColors.Accent.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = conversation.title.take(1),
                color = DemoColors.Accent,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = conversation.title,
                    color = DemoColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = conversation.updatedAtLabel,
                    color = DemoColors.TextSecondary,
                    fontSize = 13.sp,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = conversation.lastMessage,
                color = DemoColors.TextSecondary,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun ChatDetailScreen(
    conversation: ImConversation,
    messages: List<ImMessage>,
    onBack: () -> Unit,
    onSend: (String) -> Unit = {},
) {
    var draft by remember { mutableStateOf("") }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DemoColors.PageBg),
    ) {
        MineTopBar(
            title = conversation.title,
            onBack = onBack,
            containerColor = DemoColors.PageBg,
        )
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(messages, key = { it.id }) { msg ->
                MessageBubble(message = msg)
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DemoColors.Background)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            androidx.compose.material3.OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("输入消息", color = DemoColors.Muted, fontSize = 14.sp) },
                singleLine = true,
            )
            androidx.compose.material3.Button(
                onClick = {
                    val text = draft
                    if (text.isNotBlank()) {
                        onSend(text)
                        draft = ""
                    }
                },
                enabled = draft.isNotBlank(),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = DemoColors.Primary,
                    contentColor = DemoColors.OnPrimary,
                ),
            ) {
                Text("发送")
            }
        }
    }
}

@Composable
private fun MessageBubble(message: ImMessage) {
    val bubbleColor = if (message.isSelf) DemoColors.Accent else Color(0xFFE9E9EB)
    val textColor = if (message.isSelf) DemoColors.OnPrimary else DemoColors.TextPrimary
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isSelf) Alignment.End else Alignment.Start,
    ) {
        Text(
            text = "${message.senderName} · ${message.timeLabel}",
            color = DemoColors.Muted,
            fontSize = 11.sp,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Card(
            modifier = Modifier.widthIn(max = 280.dp),
            colors = CardDefaults.cardColors(containerColor = bubbleColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            shape = RoundedCornerShape(18.dp),
        ) {
            Text(
                text = message.body,
                color = textColor,
                fontSize = 17.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            )
        }
    }
}
