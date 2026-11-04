package com.ai.assistance.operit.ui.features.remote

import android.content.Context
import android.content.SharedPreferences
import android.os.Build

/**
 * 远程连接的本地配置：电脑端地址 + 配对后拿到的会话令牌。
 *
 * 这一层只读写自己的 SharedPreferences，不碰 Operit 已有的任何偏好设置，
 * 所以即使远程功能出问题，也不会影响本机 Agent 的任何行为。
 */
object RemoteConnectStore {

    private const val PREF_NAME = "operit_remote_connect"
    private const val KEY_BASE_URL = "base_url"
    private const val KEY_SESSION_TOKEN = "session_token"
    private const val KEY_DEVICE_NAME = "device_name"
    private const val KEY_LAST_PAIRED_AT = "last_paired_at"
    private const val KEY_DSH_URL = "dsh_url"

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun baseUrl(context: Context): String =
        prefs(context).getString(KEY_BASE_URL, "").orEmpty().trimEnd('/')

    fun sessionToken(context: Context): String? =
        prefs(context).getString(KEY_SESSION_TOKEN, null)?.takeIf { it.isNotBlank() }

    fun deviceName(context: Context): String {
        val saved = prefs(context).getString(KEY_DEVICE_NAME, "").orEmpty()
        return saved.ifBlank { defaultDeviceName() }
    }

    fun lastPairedAt(context: Context): Long = prefs(context).getLong(KEY_LAST_PAIRED_AT, 0L)

    fun isPaired(context: Context): Boolean =
        baseUrl(context).isNotBlank() && sessionToken(context) != null

    fun saveBaseUrl(context: Context, url: String) {
        prefs(context).edit().putString(KEY_BASE_URL, url.trim().trimEnd('/')).apply()
    }

    fun saveSession(context: Context, token: String) {
        prefs(context)
            .edit()
            .putString(KEY_SESSION_TOKEN, token)
            .putLong(KEY_LAST_PAIRED_AT, System.currentTimeMillis())
            .apply()
    }

    fun saveDeviceName(context: Context, name: String) {
        prefs(context).edit().putString(KEY_DEVICE_NAME, name.trim()).apply()
    }

    /** DeepSeek Harness 远程地址（电脑端 DSH 远程面板给出的完整地址，含 token 参数）。 */
    fun dshUrl(context: Context): String =
        prefs(context).getString(KEY_DSH_URL, "").orEmpty().trim()

    fun saveDshUrl(context: Context, url: String) {
        prefs(context).edit().putString(KEY_DSH_URL, url.trim()).apply()
    }

    /** 断开只清令牌，保留地址，方便用户重新配对时不用再粘贴一遍。 */
    fun clearSession(context: Context) {
        prefs(context).edit().remove(KEY_SESSION_TOKEN).remove(KEY_LAST_PAIRED_AT).apply()
    }

    fun clearAll(context: Context) {
        prefs(context).edit().clear().apply()
    }

    fun defaultDeviceName(): String {
        val model = Build.MODEL?.trim().orEmpty()
        return if (model.isBlank()) "Operit 手机端" else "Operit · $model"
    }
}
