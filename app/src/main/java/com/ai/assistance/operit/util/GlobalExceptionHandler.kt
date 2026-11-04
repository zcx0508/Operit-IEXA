package com.ai.assistance.operit.util

import android.content.Context
import android.content.Intent
import android.util.Log
import com.ai.assistance.operit.ui.error.CrashReportActivity
import kotlin.system.exitProcess

class GlobalExceptionHandler(private val context: Context) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, ex: Throwable) {
        // 先把崩溃现场落盘（下载目录 / Android-data / 内部目录），再做原本的跳转。
        // 这段必须放在最前面且自身绝不抛异常：某些设备上 startActivity 会因为后台启动限制
        // 直接失败，那样用户看到的就只是"应用闪一下就没了"，任何栈都留不下来。
        try {
            CrashDiagnostics.dumpCrash(context, "未捕获异常（线程 " + thread.name + "）", ex)
        } catch (_: Throwable) {
        }

        try {
            CrashRecoveryState.markPendingCrashReportLaunch(context)
        } catch (_: Throwable) {
        }

        val stackTrace = ThrowableTextFormatter.format(ex)

        try {
            val intent =
                    Intent(context, CrashReportActivity::class.java).apply {
                        putExtra(CrashReportActivity.EXTRA_STACK_TRACE, stackTrace)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    }
            context.startActivity(intent)
        } catch (e: Throwable) {
            try {
                Log.e("GlobalExceptionHandler", "启动崩溃页失败", e)
            } catch (_: Throwable) {
            }
        }

        // 终止当前进程
        exitProcess(1)
    }
}
