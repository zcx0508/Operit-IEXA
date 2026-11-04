package com.ai.assistance.operit.ui.features.remote

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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 设置 → 远程连接。
 *
 * 完全独立的一屏：不依赖 Operit 的任何全局状态，也不修改已有页面，
 * 关掉就是回到设置，出问题最多是这一屏用不了。
 */
@Composable
fun RemoteConnectScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    var paired by remember { mutableStateOf(RemoteConnectStore.isPaired(context)) }
    var notice by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                RemoteHeader(
                    title = "远程连接",
                    onBack = onClose
                )

                if (!paired) {
                    PairPane(
                        notice = notice,
                        onPaired = {
                            notice = null
                            paired = true
                        }
                    )
                } else {
                    WorkspacePane(
                        onAuthLost = { message ->
                            RemoteConnectStore.clearSession(context)
                            notice = message
                            paired = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun RemoteHeader(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "返回",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/* ----------------------------- 未配对 ----------------------------- */

@Composable
private fun PairPane(notice: String?, onPaired: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var address by remember { mutableStateOf(RemoteConnectStore.baseUrl(context)) }
    var deviceName by remember { mutableStateOf(RemoteConnectStore.deviceName(context)) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (notice != null) {
            NoticeCard(text = notice, isError = true)
        }

        SectionCard(title = "配对电脑端 IEXA") {
            Text(
                text = "在电脑端 IEXA 的「设备与桥接」里生成一次性永久连接地址，" +
                    "把整段地址粘贴到下面即可配对。配对之后手机在任意网络都能连上电脑端。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = address,
                onValueChange = {
                    address = it
                    error = null
                },
                label = { Text("连接地址") },
                placeholder = { Text("https://desktop-5jsf2gl.tail0ec667.ts.net/#pair=…") },
                singleLine = false,
                maxLines = 3,
                modifier = Modifier.fillMaxWidth(),
                enabled = !busy
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = deviceName,
                onValueChange = { deviceName = it },
                label = { Text("本机名称（电脑端会显示这个名字）") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = !busy
            )
            if (error != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = error.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.clickable(enabled = !busy) {
                        if (busy) return@clickable
                        busy = true
                        error = null
                        scope.launch {
                            try {
                                RemoteConnectApi.pair(context, address, deviceName)
                                onPaired()
                            } catch (e: Throwable) {
                                error = e.message ?: "配对失败"
                            } finally {
                                busy = false
                            }
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (busy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(
                            text = if (busy) "正在配对…" else "配对",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }

        Text(
            text = "提示：地址末尾的 /#pair=… 就是一次性配对码，配对成功后它就不再需要。" +
                "电脑端如果在「设备列表」里撤销了这台设备，这里会提示重新配对。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/* ----------------------------- 已配对 ----------------------------- */

@Composable
private fun WorkspacePane(onAuthLost: (String) -> Unit) {
    val context = LocalContext.current
    var opened by remember { mutableStateOf<RemoteSessionSummary?>(null) }

    val current = opened
    if (current == null) {
        SessionsPane(
            onOpen = { opened = it },
            onAuthLost = onAuthLost
        )
    } else {
        ChatPane(
            session = current,
            onBack = { opened = null },
            onAuthLost = onAuthLost
        )
    }
}

@Composable
private fun SessionsPane(onOpen: (RemoteSessionSummary) -> Unit, onAuthLost: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var status by remember { mutableStateOf<RemoteDeviceStatus?>(null) }
    var sessions by remember { mutableStateOf<List<RemoteSessionSummary>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun refresh() {
        scope.launch {
            loading = true
            try {
                status = RemoteConnectApi.status(context)
                sessions = RemoteConnectApi.sessions(context)
                error = null
            } catch (e: RemoteUnauthorizedException) {
                onAuthLost(e.message ?: "需要重新配对")
            } catch (e: Throwable) {
                error = e.message ?: "连接失败"
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(Unit) { refresh() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionCard(title = "连接状态") {
            val device = status
            if (loading && device == null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("正在连接电脑端…", style = MaterialTheme.typography.bodySmall)
                }
            } else if (device != null) {
                InfoLine("已连接", device.hostname, highlight = true)
                InfoLine("平台", device.platform)
                InfoLine("本机在电脑端的名字", device.deviceName)
                if (device.workingDir.isNotBlank()) InfoLine("工作目录", device.workingDir)
            } else {
                Text(
                    text = error ?: "尚未连上电脑端",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(
                    onClick = {
                        busy = true
                        scope.launch {
                            try {
                                val created = RemoteConnectApi.createSession(context)
                                sessions = RemoteConnectApi.sessions(context)
                                onOpen(created)
                            } catch (e: RemoteUnauthorizedException) {
                                onAuthLost(e.message ?: "需要重新配对")
                            } catch (e: Throwable) {
                                error = e.message ?: "新建会话失败"
                            } finally {
                                busy = false
                            }
                        }
                    },
                    enabled = !busy
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("新建会话")
                }
                TextButton(onClick = { refresh() }, enabled = !loading) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("刷新")
                }
                TextButton(
                    onClick = {
                        RemoteConnectStore.clearSession(context)
                        onAuthLost("已断开这台设备，需要时可以重新配对")
                    }
                ) {
                    Text("断开", color = MaterialTheme.colorScheme.error)
                }
            }
        }

        if (error != null && status != null) {
            NoticeCard(text = error.orEmpty(), isError = true)
        }

        Text(
            text = "电脑端会话",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        if (sessions.isEmpty() && !loading) {
            Text(
                text = "电脑端还没有会话，点「新建会话」开一个。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(sessions, key = { it.id }) { session ->
                SessionRow(session = session, onClick = { onOpen(session) })
            }
        }
    }
}

@Composable
private fun SessionRow(session: RemoteSessionSummary, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(
                text = session.title.ifBlank { "未命名会话" },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = buildString {
                    append("${session.messageCount} 条消息")
                    if (session.pinned) append(" · 已置顶")
                    val time = relativeTime(session.updatedAt)
                    if (time.isNotBlank()) append(" · $time")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/* ----------------------------- 会话对话 ----------------------------- */

@Composable
private fun ChatPane(
    session: RemoteSessionSummary,
    onBack: () -> Unit,
    onAuthLost: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val messages = remember { mutableStateListOf<RemoteChatMessage>() }
    val listState = rememberLazyListState()

    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(session.id) {
        loading = true
        try {
            val history = RemoteConnectApi.history(context, session.id)
            messages.clear()
            messages.addAll(history)
            error = null
        } catch (e: RemoteUnauthorizedException) {
            onAuthLost(e.message ?: "需要重新配对")
        } catch (e: Throwable) {
            error = e.message ?: "读取会话失败"
        } finally {
            loading = false
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.scrollToItem(messages.size - 1)
    }

    fun send() {
        val text = draft.trim()
        if (text.isEmpty() || busy) return
        draft = ""
        busy = true
        error = null
        messages.add(RemoteChatMessage("user", text))
        scope.launch {
            try {
                RemoteConnectApi.send(context, session.id, text)
                var lastAssistant = ""
                var stableRounds = 0
                val deadline = System.currentTimeMillis() + 180_000L
                while (System.currentTimeMillis() < deadline) {
                    delay(1200)
                    val latest = RemoteConnectApi.history(context, session.id)
                    messages.clear()
                    messages.addAll(latest)
                    val current = latest.lastOrNull { it.role == "assistant" }?.content.orEmpty()
                    if (current.isNotEmpty() && current == lastAssistant) {
                        stableRounds += 1
                        if (stableRounds >= 2) break
                    } else {
                        stableRounds = 0
                        lastAssistant = current
                    }
                    if (!busy) break
                }
            } catch (e: RemoteUnauthorizedException) {
                onAuthLost(e.message ?: "需要重新配对")
            } catch (e: Throwable) {
                error = e.message ?: "发送失败"
            } finally {
                busy = false
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) { Text("← 会话列表") }
            Spacer(Modifier.weight(1f))
            Text(
                text = session.title.ifBlank { "未命名会话" },
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 200.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            when {
                loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                }

                messages.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "这个会话还是空的，说点什么吧。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages.size) { index ->
                        Bubble(message = messages[index])
                    }
                }
            }
        }

        if (busy) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "电脑端正在执行…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (error != null) {
            Text(
                text = error.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                placeholder = { Text("发给电脑端 IEXA…") },
                modifier = Modifier.weight(1f),
                maxLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { send() })
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = { send() },
                enabled = draft.isNotBlank() && !busy,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "发送",
                    tint = if (draft.isNotBlank() && !busy) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

@Composable
private fun Bubble(message: RemoteChatMessage) {
    val fromUser = message.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (fromUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (fromUser) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
            )
        }
    }
}

/* ----------------------------- 公共小件 ----------------------------- */

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            )
        ) {
            Column(modifier = Modifier.padding(14.dp)) { content() }
        }
    }
}

@Composable
private fun NoticeCard(text: String, isError: Boolean) {
    val tint = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Surface(
        color = tint.copy(alpha = 0.12f),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String, highlight: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(132.dp)
        )
        if (highlight) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(4.dp))
        }
        Text(
            text = value.ifBlank { "—" },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun relativeTime(timestamp: Long): String {
    if (timestamp <= 0L) return ""
    val diff = System.currentTimeMillis() - timestamp
    if (diff < 0) return ""
    val minute = 60_000L
    val hour = 60 * minute
    val day = 24 * hour
    return when {
        diff < minute -> "刚刚"
        diff < hour -> "${diff / minute} 分钟前"
        diff < day -> "${diff / hour} 小时前"
        diff < 7 * day -> "${diff / day} 天前"
        else -> {
            val date = java.text.SimpleDateFormat("MM-dd", java.util.Locale.getDefault())
            date.format(java.util.Date(timestamp))
        }
    }
}
