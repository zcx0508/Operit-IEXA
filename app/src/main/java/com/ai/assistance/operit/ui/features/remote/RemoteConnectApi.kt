package com.ai.assistance.operit.ui.features.remote

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

data class RemoteDeviceStatus(
    val hostname: String,
    val platform: String,
    val workingDir: String,
    val deviceName: String
)

data class RemoteSessionSummary(
    val id: String,
    val title: String,
    val messageCount: Int,
    val pinned: Boolean,
    val updatedAt: Long
)

data class RemoteChatMessage(
    val role: String,
    val content: String
)

/** 电脑端自建的 DeepSeek Harness 远程入口状态。 */
data class DshHostStatus(
    val running: Boolean,
    val url: String,
    val platform: String,
    val hostname: String,
    val workDir: String,
    val detail: String
)

/** 令牌被电脑端撤销 / 过期时抛这个，UI 用它回到配对界面。 */
class RemoteUnauthorizedException(message: String) : IOException(message)

/**
 * 电脑端 IEXA 的远程接口客户端。
 *
 * 只用到 Operit 已经有的 okhttp + org.json，不引入新依赖；
 * 请求头沿用电脑端要求的 Authorization: Bearer <sessionToken> 与 Origin。
 */
object RemoteConnectApi {

    private val jsonMediaType = "application/json".toMediaType()

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .callTimeout(0, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    private fun buildRequest(
        context: Context,
        path: String,
        method: String,
        body: JSONObject? = null,
        withAuth: Boolean = true
    ): Request {
        val base = RemoteConnectStore.baseUrl(context)
        val builder = Request.Builder().url(base + path)
        if (withAuth) {
            RemoteConnectStore.sessionToken(context)?.let { builder.header("Authorization", "Bearer $it") }
        }
        // 电脑端会校验 Origin，挡掉非本应用的网页跨域请求
        builder.header("Origin", base)
        if (body != null) {
            builder.header("Content-Type", "application/json")
            builder.method(method, body.toString().toRequestBody(jsonMediaType))
        } else {
            builder.method(method, null)
        }
        return builder.build()
    }

    private suspend fun execute(request: Request): JSONObject = withContext(Dispatchers.IO) {
        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            val payload = if (text.isBlank()) {
                JSONObject()
            } else {
                runCatching { JSONObject(text) }.getOrElse { JSONObject().put("raw", text) }
            }
            if (response.code == 401) {
                throw RemoteUnauthorizedException(
                    payload.optString("error", "此设备尚未与 IEXA 配对，或配对已被撤销")
                )
            }
            if (!response.isSuccessful) {
                throw IOException(payload.optString("error", "请求失败：HTTP ${response.code}"))
            }
            payload
        }
    }

    /**
     * 用一次性连接地址配对，成功后把会话令牌存进本地。
     * 地址形如 https://xxx/#pair=<token>，也兼容 https://xxx/?pair=<token>。
     */
    suspend fun pair(context: Context, pairUrl: String, deviceName: String): Unit {
        val raw = pairUrl.trim()
        if (raw.isBlank()) throw IllegalArgumentException("请先粘贴电脑端给出的连接地址")

        var token = raw.substringAfter("#pair=", "")
        if (token.isBlank()) token = raw.substringAfter("?pair=", "")
        if (token.isBlank()) throw IllegalArgumentException("地址里缺少配对码，请把电脑端给的整段地址完整粘贴进来")

        val base = raw.substringBefore("/#pair=").substringBefore("/?pair=").trimEnd('/')
        if (!base.startsWith("http://") && !base.startsWith("https://")) {
            throw IllegalArgumentException("连接地址必须以 http:// 或 https:// 开头")
        }

        RemoteConnectStore.saveBaseUrl(context, base)
        RemoteConnectStore.saveDeviceName(context, deviceName.trim().ifBlank { RemoteConnectStore.defaultDeviceName() })

        val payload = JSONObject()
            .put("token", token)
            .put("name", RemoteConnectStore.deviceName(context))

        val response = execute(
            buildRequest(context, "/api/mobile-bridge/pair", "POST", payload, withAuth = false)
        )
        val sessionToken = response.optString("sessionToken", "")
        if (sessionToken.isBlank()) throw IOException("电脑端没有返回配对令牌，请确认地址是否是电脑端当前给出的那一个")
        RemoteConnectStore.saveSession(context, sessionToken)
    }

    suspend fun status(context: Context): RemoteDeviceStatus = withContext(Dispatchers.IO) {
        val payload = execute(buildRequest(context, "/api/remote/status", "GET"))
        val device = payload.optJSONObject("device")
        RemoteDeviceStatus(
            hostname = payload.optString("hostname", "?"),
            platform = payload.optString("platform", "?"),
            workingDir = payload.optString("cwd", ""),
            deviceName = device?.optString("name", "?") ?: "?"
        )
    }

    suspend fun sessions(context: Context): List<RemoteSessionSummary> = withContext(Dispatchers.IO) {
        val payload = execute(buildRequest(context, "/api/sessions", "GET"))
        val array = payload.optJSONArray("sessions") ?: JSONArray()
        (0 until array.length()).mapNotNull { index ->
            val item = array.optJSONObject(index) ?: return@mapNotNull null
            val id = item.optString("id", "")
            if (id.isBlank()) return@mapNotNull null
            RemoteSessionSummary(
                id = id,
                title = item.optString("title", "未命名会话"),
                messageCount = item.optInt("messageCount", 0),
                pinned = item.optBoolean("pinned", false),
                updatedAt = item.optLong("updated", 0L)
            )
        }.sortedWith(compareByDescending<RemoteSessionSummary> { it.pinned }.thenByDescending { it.updatedAt })
    }

    suspend fun createSession(context: Context): RemoteSessionSummary = withContext(Dispatchers.IO) {
        val payload = execute(buildRequest(context, "/api/sessions", "POST", JSONObject()))
        val session = payload.optJSONObject("session") ?: throw IOException("电脑端没有返回新建的会话")
        val id = session.optString("id", "")
        if (id.isBlank()) throw IOException("电脑端返回的会话缺少 id")
        RemoteSessionSummary(
            id = id,
            title = session.optString("title", "新会话"),
            messageCount = session.optInt("messageCount", 0),
            pinned = session.optBoolean("pinned", false),
            updatedAt = session.optLong("updated", 0L)
        )
    }

    suspend fun history(context: Context, sessionId: String): List<RemoteChatMessage> =
        withContext(Dispatchers.IO) {
            val payload = execute(buildRequest(context, "/api/sessions/$sessionId", "GET"))
            val array = payload.optJSONArray("messages") ?: JSONArray()
            val messages = ArrayList<RemoteChatMessage>()
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val role = item.optString("role", "")
                if (role != "user" && role != "assistant") continue
                val content = extractContent(item)
                if (content.isNotBlank()) messages.add(RemoteChatMessage(role, content))
            }
            messages
        }

    suspend fun send(context: Context, sessionId: String, message: String) {
        withContext(Dispatchers.IO) {
            execute(
                buildRequest(
                    context,
                    "/api/chat",
                    "POST",
                    JSONObject().put("sessionId", sessionId).put("message", message)
                )
            )
        }
    }

    /**
     * 电脑端网页界面的手机入口：`/mobile-web?token=<设备会话>`。
     *
     * 电脑端会校验设备令牌、种下 cookie 再跳到首页，之后页面里的实时接口（SSE）就能直接用，
     * 所以手机端不需要自己轮询对话内容。
     */
    fun webEntryUrl(context: Context): String {
        val base = RemoteConnectStore.baseUrl(context)
        val token = RemoteConnectStore.sessionToken(context) ?: return ""
        if (base.isBlank()) return ""
        return "$base/mobile-web?token=${java.net.URLEncoder.encode(token, "UTF-8")}"
    }

    /**
     * 电脑端自建的 DeepSeek Harness 远程入口状态。
     *
     * 由 IEXA 电脑端起一个 dsh web 独立实例并用 tailscale serve 挂到内网，
     * 这里拿到的是「带 token 的访问地址 + 平台 / 主机名 / 工作目录」，用来渲染配对卡。
     */
    suspend fun dshHostStatus(context: Context): DshHostStatus = withContext(Dispatchers.IO) {
        parseDshHost(execute(buildRequest(context, "/api/dsh-host/status", "GET")))
    }

    suspend fun dshHostStart(context: Context): DshHostStatus = withContext(Dispatchers.IO) {
        parseDshHost(execute(buildRequest(context, "/api/dsh-host/start", "POST", JSONObject())))
    }

    suspend fun dshHostStop(context: Context): DshHostStatus = withContext(Dispatchers.IO) {
        parseDshHost(execute(buildRequest(context, "/api/dsh-host/stop", "POST", JSONObject())))
    }

    private fun parseDshHost(payload: JSONObject): DshHostStatus = DshHostStatus(
        running = payload.optBoolean("running", false),
        url = payload.optString("url", ""),
        platform = payload.optString("platform", ""),
        hostname = payload.optString("hostname", ""),
        workDir = payload.optString("workDir", ""),
        detail = payload.optString("detail", "")
    )

    /** 消息正文可能是纯字符串，也可能是分段数组，两种都兼容。 */
    private fun extractContent(item: JSONObject): String {
        val content = item.opt("content")
        if (content is String) return content
        val array = item.optJSONArray("content") ?: return ""
        val builder = StringBuilder()
        for (index in 0 until array.length()) {
            val part = array.optJSONObject(index) ?: continue
            val text = part.optString("text", "")
            if (text.isBlank()) continue
            if (builder.isNotEmpty()) builder.append('\n')
            builder.append(text)
        }
        return builder.toString()
    }
}
