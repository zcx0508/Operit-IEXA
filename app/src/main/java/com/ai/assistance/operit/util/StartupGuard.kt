package com.ai.assistance.operit.util

import android.content.Context

/**
 * 启动守卫：识别"上一次启动没走完"（闪退 / 被系统杀掉 / 被用户强退），
 * 下一次启动自动进入**安全模式** —— 跳过全部后台预加载，先把界面开起来。
 *
 * 设计取舍：
 * - 用 SharedPreferences.commit()（同步写）而不是 apply()，因为崩溃随时可能发生，
 *   异步落盘有概率丢失计数，那样就永远进不了安全模式。
 * - 只要"界面起来并稳定几秒"就清零计数，所以正常使用时不会平白无故进安全模式；
 *   反过来，如果安全模式这一次也崩，计数会继续累积，下次仍然是安全模式（不会来回抖）。
 * - 全部包 try/catch，任何异常都退化成"正常启动"。
 */
object StartupGuard {

    private const val PREFS_NAME = "operit_startup_guard"
    private const val KEY_ATTEMPTS = "unfinished_attempts"
    private const val KEY_COMPLETED_AT = "last_completed_at"
    private const val KEY_LAST_SAFE = "last_launch_safe_mode"

    /** 连续多少次"没走完"就进入紧急诊断模式 */
    private const val EMERGENCY_THRESHOLD = 3

    /** 本次启动是否处于安全模式。main 线程在 Application.onCreate 里写入，之后只读。 */
    @Volatile
    var isSafeMode: Boolean = false
        private set

    /**
     * 本进程是否已经做过"本次启动"的判定。
     * 现在最早的调用点是 [com.ai.assistance.operit.provider.BootProbeProvider]（provider 阶段），
     * Application.onCreate 里会再调一次，这里保证只判定一次、计数只 +1。
     */
    @Volatile
    private var begun = false

    /**
     * 进程最早期调用。返回 true 表示本次进入安全模式。
     * 重复调用（provider 之后 Application 再调）直接返回已判定的结果。
     */
    @JvmStatic
    fun beginLaunch(context: Context?): Boolean {
        if (begun) return isSafeMode
        val ctx = context ?: return false
        return try {
            begun = true
            val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val attempts = prefs.getInt(KEY_ATTEMPTS, 0)
            val safe = attempts >= 1
            prefs
                .edit()
                .putInt(KEY_ATTEMPTS, attempts + 1)
                .putBoolean(KEY_LAST_SAFE, safe)
                .commit()
            isSafeMode = safe
            safe
        } catch (_: Throwable) {
            isSafeMode = false
            false
        }
    }

    /**
     * 界面已经起来并稳定运行几秒后调用，把"未完成"计数清零。
     */
    @JvmStatic
    fun markLaunchCompleted(context: Context?) {
        val ctx = context ?: return
        try {
            ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putInt(KEY_ATTEMPTS, 0)
                .putLong(KEY_COMPLETED_AT, System.currentTimeMillis())
                .putBoolean(KEY_LAST_SAFE, false)
                .commit()
        } catch (_: Throwable) {
        }
    }

    /** 上一次启动是否走了安全模式（用于提示用户把这情况告诉开发） */
    @JvmStatic
    fun wasLastLaunchSafeMode(context: Context?): Boolean {
        val ctx = context ?: return false
        return try {
            ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_LAST_SAFE, false)
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * 紧急模式：连续 [EMERGENCY_THRESHOLD] 次启动都没走完（连安全模式也没救回来），
     * 说明问题很可能就在主界面本身（Compose 组合/首帧）。
     * 这时 MainActivity 会跳过整个主界面，改用纯 View 的诊断页 —— 保证"一定能打开、能看到原因"。
     */
    @JvmStatic
    fun isEmergencyMode(context: Context?): Boolean {
        val ctx = context ?: return false
        return try {
            ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getInt(KEY_ATTEMPTS, 0) >= EMERGENCY_THRESHOLD
        } catch (_: Throwable) {
            false
        }
    }

    /** 用户在紧急诊断页点"重试完整启动"时调用：清零计数 */
    @JvmStatic
    fun resetForRetry(context: Context?) {
        val ctx = context ?: return
        try {
            ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putInt(KEY_ATTEMPTS, 0)
                .putBoolean(KEY_LAST_SAFE, false)
                .commit()
            begun = false
            isSafeMode = false
        } catch (_: Throwable) {
        }
    }
}
