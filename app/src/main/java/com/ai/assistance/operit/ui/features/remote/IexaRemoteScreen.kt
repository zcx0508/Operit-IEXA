package com.ai.assistance.operit.ui.features.remote

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch

/**
 * IEXA 远程（手机端）。
 *
 * 配对前：一张配对卡（粘贴电脑端地址即可）。
 * 配对后：和 DSH 那一屏完全同构的状态卡 + 「打开远程界面」——直接内嵌玩电脑端的网页界面，
 * 对话走网页自己的实时通道（SSE），所以内容是实时刷新的，不用手动刷新、也不用等半天。
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun IexaRemoteScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var paired by remember { mutableStateOf(RemoteConnectStore.isPaired(context)) }
    var status by remember { mutableStateOf<RemoteDeviceStatus?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var address by remember { mutableStateOf(RemoteConnectStore.baseUrl(context)) }
    var deviceName by remember { mutableStateOf(RemoteConnectStore.deviceName(context)) }
    var progress by remember { mutableStateOf(0) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var openedUrl by remember { mutableStateOf("") }

    fun refresh() {
        if (busy) return
        busy = true
        error = null
        scope.launch {
            try {
                status = RemoteConnectApi.status(context)
                paired = true
            } catch (e: RemoteUnauthorizedException) {
                paired = false
                RemoteConnectStore.clearSession(context)
                error = e.message ?: "配对已失效，请重新配对"
            } catch (e: Exception) {
                error = e.message ?: "读取电脑端状态失败"
            } finally {
                busy = false
            }
        }
    }

    LaunchedEffect(paired) { if (paired) refresh() }

    val back = {
        val view = webView
        if (view != null && view.canGoBack()) {
            view.goBack()
        } else if (openedUrl.isNotBlank()) {
            openedUrl = ""
        } else {
            onClose()
        }
    }
    BackHandler(enabled = true) { back() }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.fillMaxSize().statusBarsPadding().imePadding()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(onClick = { back() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "IEXA 远程",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    IconButton(
                        onClick = {
                            if (openedUrl.isNotBlank()) {
                                progress = 0
                                webView?.reload()
                            } else {
                                refresh()
                            }
                        }
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = "刷新")
                    }
                }

                if (busy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

                if (openedUrl.isNotBlank()) {
                    if (progress in 1..99) {
                        LinearProgressIndicator(
                            progress = { progress / 100f },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            WebView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.databaseEnabled = true
                                settings.useWideViewPort = true
                                settings.loadWithOverviewMode = true
                                settings.mediaPlaybackRequiresUserGesture = false
                                CookieManager.getInstance().setAcceptCookie(true)
                                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                                webViewClient = object : WebViewClient() {
                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        progress = 100
                                    }

                                    override fun onReceivedError(
                                        view: WebView?,
                                        request: android.webkit.WebResourceRequest?,
                                        errorInfo: android.webkit.WebResourceError?
                                    ) {
                                        super.onReceivedError(view, request, errorInfo)
                                        if (request?.isForMainFrame == true) progress = 100
                                    }
                                }
                                webChromeClient = object : WebChromeClient() {
                                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                        progress = newProgress
                                    }
                                }
                                webView = this
                                loadUrl(openedUrl)
                            }
                        },
                        update = { view -> webView = view }
                    )
                } else {
                    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        Text(
                            text = if (paired) "连接状态" else "配对电脑端 IEXA",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 8.dp)
                        )
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                if (paired) {
                                    val current = status
                                    StatusLine(
                                        label = if (current != null) "已连接" else "未连接",
                                        value = when {
                                            current == null -> "读取中…"
                                            else -> "✓ ${current.hostname}"
                                        },
                                        emphasize = current != null
                                    )
                                    Spacer(Modifier.height(10.dp))
                                    StatusLine("平台", current?.platform ?: "—")
                                    Spacer(Modifier.height(10.dp))
                                    StatusLine("本机在电脑端的名字", current?.deviceName ?: deviceName)
                                    Spacer(Modifier.height(10.dp))
                                    StatusLine("工作目录", current?.workingDir?.ifBlank { "—" } ?: "—")
                                    Spacer(Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        TextButton(onClick = { refresh() }, enabled = !busy) { Text("刷新") }
                                        TextButton(
                                            onClick = {
                                                val url = RemoteConnectApi.webEntryUrl(context)
                                                if (url.isBlank()) {
                                                    error = "还没配对，先在上面配对"
                                                } else {
                                                    error = null
                                                    progress = 0
                                                    openedUrl = url
                                                }
                                            },
                                            enabled = !busy
                                        ) { Text("打开远程界面") }
                                        TextButton(
                                            onClick = {
                                                RemoteConnectStore.clearSession(context)
                                                status = null
                                                paired = false
                                                openedUrl = ""
                                                error = null
                                            }
                                        ) { Text("断开") }
                                    }
                                } else {
                                    OutlinedTextField(
                                        value = address,
                                        onValueChange = { address = it; error = null },
                                        label = { Text("电脑端连接地址") },
                                        placeholder = { Text("https://…/#pair=…") },
                                        singleLine = false,
                                        maxLines = 3,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = deviceName,
                                        onValueChange = { deviceName = it },
                                        label = { Text("本机在电脑端的名字") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        TextButton(
                                            onClick = {
                                                busy = true
                                                error = null
                                                scope.launch {
                                                    try {
                                                        RemoteConnectApi.pair(context, address, deviceName)
                                                        paired = true
                                                    } catch (e: Exception) {
                                                        error = e.message ?: "配对失败"
                                                    } finally {
                                                        busy = false
                                                    }
                                                }
                                            },
                                            enabled = !busy
                                        ) { Text("配对") }
                                    }
                                }
                            }
                        }

                        error?.let { message ->
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                            )
                        }

                        Text(
                            text = "配对后点「打开远程界面」，就在这里直接用电脑端的 IEXA 网页界面；" +
                                "对话是实时推送的，不用手动刷新。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusLine(label: String, value: String, emphasize: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(150.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = if (emphasize) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (emphasize) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
    }
}
