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
 * DeepSeek Harness 远程：和 IEXA 远程同样的一张配对卡。
 *
 * 数据来自电脑端 IEXA 自建的 DSH 入口（`dsh web` 独立实例 + tailscale serve）：
 * 手机只跟已配对的 IEXA 说话，拿回「连接状态 / 平台 / 本机名字 / 工作目录」和带 token 的地址，
 * 点「打开远程界面」时再用内嵌 WebView 加载它。
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun DshRemoteScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var status by remember { mutableStateOf<DshHostStatus?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var progress by remember { mutableStateOf(0) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var openedUrl by remember { mutableStateOf("") }

    fun load(autoStart: Boolean) {
        if (busy) return
        busy = true
        error = null
        scope.launch {
            try {
                var current = RemoteConnectApi.dshHostStatus(context)
                if (!current.running && autoStart) {
                    current = RemoteConnectApi.dshHostStart(context)
                }
                status = current
                if (!current.running && current.detail.isNotBlank()) error = current.detail
            } catch (e: RemoteUnauthorizedException) {
                error = e.message ?: "此设备尚未与电脑端 IEXA 配对，请先在上面完成「IEXA 远程」的配对。"
            } catch (e: Exception) {
                error = e.message ?: "读取电脑端状态失败"
            } finally {
                busy = false
            }
        }
    }

    LaunchedEffect(Unit) { load(autoStart = true) }

    val back = {
        val view = webView
        if (view != null && view.canGoBack()) view.goBack() else if (openedUrl.isNotBlank()) {
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
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
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
                        text = "DeepSeek Harness 远程",
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
                                load(autoStart = false)
                            }
                        }
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = "刷新")
                    }
                }

                if (busy) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

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
                    val current = status
                    Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = "连接状态",
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
                                StatusRow(
                                    label = if (current?.running == true) "已连接" else "未连接",
                                    value = when {
                                        current == null -> "读取中…"
                                        current.running -> "✓ ${current.hostname}"
                                        else -> current.detail.ifBlank { "电脑端还没有启动 DSH 入口" }
                                    },
                                    emphasize = current?.running == true
                                )
                                Spacer(Modifier.height(10.dp))
                                StatusRow("平台", current?.platform?.ifBlank { "—" } ?: "—")
                                Spacer(Modifier.height(10.dp))
                                StatusRow(
                                    "本机在电脑端的名字",
                                    RemoteConnectStore.deviceName(context).ifBlank { "—" }
                                )
                                Spacer(Modifier.height(10.dp))
                                StatusRow("工作目录", current?.workDir?.ifBlank { "—" } ?: "—")

                                Spacer(Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    TextButton(onClick = { load(autoStart = true) }, enabled = !busy) {
                                        Text(if (current?.running == true) "刷新" else "启动")
                                    }
                                    TextButton(
                                        onClick = {
                                            val url = current?.url.orEmpty()
                                            if (url.isBlank()) {
                                                error = "电脑端还没有可用的远程地址"
                                            } else {
                                                error = null
                                                progress = 0
                                                openedUrl = url
                                            }
                                        },
                                        enabled = current?.running == true && !busy
                                    ) { Text("打开远程界面") }
                                    TextButton(
                                        onClick = {
                                            busy = true
                                            error = null
                                            scope.launch {
                                                try {
                                                    status = RemoteConnectApi.dshHostStop(context)
                                                } catch (e: Exception) {
                                                    error = e.message ?: "停止失败"
                                                } finally {
                                                    busy = false
                                                }
                                            }
                                        },
                                        enabled = current?.running == true && !busy
                                    ) { Text("停止") }
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
                            text = "电脑端会临时起一个 DeepSeek Harness 入口并用 Tailscale 挂到内网，" +
                                "手机只要和电脑在同一个 Tailscale 网络里，点「打开远程界面」就能直接用。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                        )

                        if (!RemoteConnectStore.isPaired(context)) {
                            Text(
                                text = "提示：先用上面的「IEXA 远程」完成一次配对，这里才能连到电脑端。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusRow(label: String, value: String, emphasize: Boolean = false) {
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
