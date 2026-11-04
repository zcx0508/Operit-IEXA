package com.ai.assistance.operit.ui.features.chat.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import com.ai.assistance.operit.data.model.ChatMessage

@Stable
class ChatMessageHeightMemory {
    private val measuredHeightsPx = linkedMapOf<Long, Int>()

    fun updateMeasured(messageId: Long, heightPx: Int) {
        if (heightPx <= 0) {
            return
        }
        measuredHeightsPx[messageId] = heightPx
    }

    fun prune(validMessageIds: Set<Long>) {
        val iterator = measuredHeightsPx.entries.iterator()
        while (iterator.hasNext()) {
            if (iterator.next().key !in validMessageIds) {
                iterator.remove()
            }
        }
    }
}

@Composable
fun rememberChatMessageHeightMemory(messages: List<ChatMessage>): ChatMessageHeightMemory {
    val heightMemory = remember { ChatMessageHeightMemory() }
    // 只在消息集合真正变化时重建集合并裁剪，避免每次重组都做一次 O(n) 的分配与遍历
    // （流式输出期间页面重组非常频繁）。
    val validMessageIds =
        remember(messages.size, messages.firstOrNull()?.timestamp, messages.lastOrNull()?.timestamp) {
            messages
                .asSequence()
                .filter { it.sender == "ai" }
                .map { it.timestamp }
                .toSet()
        }

    LaunchedEffect(validMessageIds) {
        heightMemory.prune(validMessageIds)
    }
    return heightMemory
}
