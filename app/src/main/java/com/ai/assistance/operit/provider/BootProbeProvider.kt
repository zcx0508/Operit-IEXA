package com.ai.assistance.operit.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import com.ai.assistance.operit.util.CrashDiagnostics
import com.ai.assistance.operit.util.GlobalExceptionHandler
import com.ai.assistance.operit.util.StartupGuard

/**
 * 启动探针 Provider。
 *
 * 为什么需要它：Android 的初始化顺序是 **ContentProvider.onCreate() 早于 Application.onCreate()**。
 * 所以如果崩溃发生在 provider 阶段（例如 MLKit / JLatexMath / Shizuku 的 init provider、
 * 或者我们自己 DocumentsProvider 的静态初始化），`Application.onCreate` 里装的面包屑、
 * 全局异常处理器、看门狗**一个都还没生效**，现场就完全丢掉 —— 表现就是"闪退且什么都没留下"。
 *
 * 这个 provider 在 manifest 里声明成第一个（initOrder=1），于是它是全进程第一个跑起来的代码：
 * 1. 先绑定面包屑文件并写一行（`attachTraceFile` 做了幂等，Application 里不会重复截断）；
 * 2. 立刻装全局异常处理器 —— provider 阶段的崩溃也能抓到完整栈；
 * 3. 启动主线程卡死看门狗；
 * 4. 上报 `provider` 启动信号，并补发上次没发出去的 dump。
 *
 * 全部 try/catch：这个 provider 本身绝不能成为新的崩溃源。
 */
class BootProbeProvider : ContentProvider() {

    override fun onCreate(): Boolean {
        val ctx = context ?: return true
        try {
            CrashDiagnostics.attachTraceFile(ctx)
            CrashDiagnostics.markStep("provider 阶段：BootProbeProvider.onCreate（进程最早期）")
        } catch (_: Throwable) {
        }
        try {
            // 提前装：覆盖 provider 阶段与 Application.onCreate 早期抛出的异常
            if (Thread.getDefaultUncaughtExceptionHandler() !is GlobalExceptionHandler) {
                Thread.setDefaultUncaughtExceptionHandler(GlobalExceptionHandler(ctx))
            }
            CrashDiagnostics.markStep("provider 阶段：全局异常处理器已提前安装")
        } catch (_: Throwable) {
        }
        try {
            StartupGuard.beginLaunch(ctx)
            CrashDiagnostics.startMainThreadStallWatchdog(ctx)
        } catch (_: Throwable) {
        }
        try {
            CrashDiagnostics.reportBoot(ctx, "provider")
            CrashDiagnostics.flushOutboxAsync(ctx)
            // 系统自己的「进程死亡档案」：上一次（含原生崩溃）的退出原因 + tombstone。
            // 这是定位「进程消失但没有 Java 异常」这类闪退的唯一可靠入口。
            com.ai.assistance.operit.util.ExitInfoHarvester.harvestAsync(ctx)
        } catch (_: Throwable) {
        }
        return true
    }

    // 这个 provider 不对外提供任何数据，只是"占位触发器"
    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int = 0
}
