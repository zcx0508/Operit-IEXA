package com.ai.assistance.operit.util

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.os.Build
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 用系统自己的「进程死亡档案」定位闪退 —— `ApplicationExitInfo`。
 *
 * 为什么需要它：这次实测到的故障形态是「进程消失、但没有产生任何 Java 异常 dump」，
 * 也就是 Java 层的 UncaughtExceptionHandler 根本没被触发。这类死亡只有系统知道原因：
 *   - REASON_CRASH_NATIVE：原生崩溃（SIGSEGV/SIGABRT），`traceInputStream` 里就是 tombstone；
 *   - REASON_CRASH：Java 崩溃，带栈；
 *   - REASON_ANR / REASON_KILLED / REASON_SIGNALED / REASON_LOW_MEMORY …
 *
 * 进程启动时把上一次（或上几次）的退出原因 + 系统保存的 trace 读出来，
 * 落盘到「下载」目录并回传开发机。全程 try/catch，绝不影响启动。
 */
object ExitInfoHarvester {

    private const val PREFS = "operit_crash_diag"
    private const val KEY_LAST_TS = "exitinfo_last_ts"
    private const val MAX_TRACE_CHARS = 220_000

    private val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    private val stampFormat = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US)

    @JvmStatic
    fun harvestAsync(context: Context?) {
        val ctx = context?.applicationContext ?: return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return // API 30 以下没有这套接口
        try {
            Thread {
                try {
                    harvest(ctx)
                } catch (_: Throwable) {
                }
            }.also {
                it.isDaemon = true
                it.name = "OperitExitInfoHarvest"
            }.start()
        } catch (_: Throwable) {
        }
    }

    private fun harvest(ctx: Context) {
        val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return
        val infos = am.getHistoricalProcessExitReasons(ctx.packageName, 0, 8) ?: return
        if (infos.isEmpty()) return

        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val lastTs = prefs.getLong(KEY_LAST_TS, 0L)

        val sb = StringBuilder()
        sb.append("=== Operit 历史进程退出原因（ApplicationExitInfo，系统自己记录的死亡档案）===\n")
        sb.append("导出时间: ").append(timeFormat.format(Date())).append('\n')
        try {
            sb.append("设备: ").append(Build.MANUFACTURER).append(' ').append(Build.MODEL)
                .append("  Android ").append(Build.VERSION.RELEASE)
                .append(" (SDK ").append(Build.VERSION.SDK_INT).append(")\n")
            sb.append("ROM: ").append(Build.DISPLAY).append('\n')
        } catch (_: Throwable) {
        }
        sb.append("说明: 只导出上次上报之后的新记录。reason=4 是原生崩溃，此时下方会带 tombstone。\n\n")

        var newest = lastTs
        var count = 0
        for (info in infos) {
            if (info.timestamp <= lastTs) continue
            count++
            if (info.timestamp > newest) newest = info.timestamp

            sb.append("---- 退出记录 #").append(count).append(" ----\n")
            sb.append("时间: ").append(timeFormat.format(Date(info.timestamp))).append('\n')
            sb.append("原因: ").append(reasonName(info.reason)).append(" (").append(info.reason).append(")\n")
            sb.append("描述: ").append(info.description ?: "（无）").append('\n')
            try {
                sb.append("pid=").append(info.pid)
                    .append("  importance=").append(info.importance)
                    .append("  status=").append(info.status)
                    .append("  pss=").append(info.pss)
                    .append("  rss=").append(info.rss).append('\n')
            } catch (_: Throwable) {
            }

            try {
                val stream = info.traceInputStream
                if (stream != null) {
                    val text = stream.use { String(it.readBytes(), Charsets.UTF_8) }
                    sb.append("---- 系统保存的现场（tombstone / 崩溃栈）----\n")
                    sb.append(
                        if (text.length > MAX_TRACE_CHARS) text.substring(0, MAX_TRACE_CHARS) + "\n...(截断)"
                        else text
                    )
                    if (!text.endsWith("\n")) sb.append('\n')
                } else {
                    sb.append("（系统没有为这个退出保存 trace）\n")
                }
            } catch (t: Throwable) {
                sb.append("读取 trace 失败: ").append(t.javaClass.name).append(": ").append(t.message).append('\n')
            }
            sb.append('\n')
        }

        if (count == 0) return
        val body = sb.toString()
        try {
            CrashDiagnostics.dumpToDownloadsAndUpload(ctx, "exitinfo", "Operit-exitinfo", body)
        } catch (_: Throwable) {
        }
        try {
            prefs.edit().putLong(KEY_LAST_TS, newest).apply()
        } catch (_: Throwable) {
        }
    }

    private fun reasonName(reason: Int): String = when (reason) {
        ApplicationExitInfo.REASON_EXIT_SELF -> "REASON_EXIT_SELF(自己主动退出)"
        ApplicationExitInfo.REASON_SIGNALED -> "REASON_SIGNALED(收到信号)"
        ApplicationExitInfo.REASON_LOW_MEMORY -> "REASON_LOW_MEMORY(低内存被杀)"
        ApplicationExitInfo.REASON_CRASH -> "REASON_CRASH(Java 崩溃)"
        ApplicationExitInfo.REASON_CRASH_NATIVE -> "REASON_CRASH_NATIVE(原生崩溃)"
        ApplicationExitInfo.REASON_ANR -> "REASON_ANR(无响应)"
        ApplicationExitInfo.REASON_INITIALIZATION_FAILURE -> "REASON_INITIALIZATION_FAILURE(初始化失败)"
        ApplicationExitInfo.REASON_PERMISSION_CHANGE -> "REASON_PERMISSION_CHANGE(权限变更)"
        ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE -> "REASON_EXCESSIVE_RESOURCE_USAGE(资源超限)"
        ApplicationExitInfo.REASON_USER_REQUESTED -> "REASON_USER_REQUESTED(用户要求)"
        ApplicationExitInfo.REASON_USER_STOPPED -> "REASON_USER_STOPPED(用户强行停止)"
        ApplicationExitInfo.REASON_DEPENDENCY_DIED -> "REASON_DEPENDENCY_DIED(依赖进程死亡)"
        ApplicationExitInfo.REASON_OTHER -> "REASON_OTHER(其他)"
        ApplicationExitInfo.REASON_FREEZER -> "REASON_FREEZER(冻结)"
        else -> "UNKNOWN"
    }
}
