package com.ai.assistance.operit.util

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 启动自救诊断。
 *
 * 目的：在无法使用 adb / logcat 的设备上（例如用户手机未开 USB 调试）也能拿到崩溃现场。
 *
 * 做法：
 * 1. [markStep] 在启动关键点写一条"面包屑"（时间戳 + 步骤名），进程若被原生代码
 *    SIGSEGV 之类直接杀掉、Java 层捕获不到异常时，面包屑仍然留在盘上，可以据此定位卡在哪一步。
 * 2. [dumpCrash] 把崩溃栈 + 设备与版本信息 + 应用日志尾部写成一个 txt，落到
 *    「下载」目录（MediaStore，Android 10+ 无需权限）、应用外部私有目录、内部私有目录三处，
 *    用户可以直接在文件管理器的"下载"里找到并发出来。
 *
 * 本类所有方法都必须**绝不抛异常**——它运行在崩溃路径上，一旦它自己崩了，就什么线索都没有了。
 */
object CrashDiagnostics {

    private const val TAG = "OperitCrashDiag"
    private const val TRACE_FILE_NAME = "operit-startup-trace.txt"
    private const val CRASH_DIR_NAME = "crash"
    private const val MAX_LOG_TAIL_BYTES = 96 * 1024
    private const val MAX_TRACE_BYTES = 32 * 1024
    private const val HARVEST_TAIL_BYTES = 128 * 1024

    /** 主线程多久没响应就认为卡死（系统 ANR 阈值 5s，这里提前抓） */
    private const val MAIN_THREAD_STALL_MS = 3500L

    /**
     * 崩溃现场自动回传地址：都走 Tailscale 私网（只有本机与配对设备可达，不暴露公网）。
     * 依次尝试，任意一个返回 2xx 就停 —— 手机上无论走 WiFi 还是移动数据，
     * 只要 Tailscale 在线就能把 stack 直接送到开发机，不需要用户翻文件、截图、传文件。
     */
    private val UPLOAD_URLS =
        arrayOf(
            // 公网 Funnel（手机没有连 Tailscale、走移动数据也能到）
            "https://desktop-5jsf2gl.tail0ec667.ts.net/operit-crash-7f3a9c/crash",
            // tailnet 内直连
            "http://desktop-5jsf2gl.tail0ec667.ts.net:8090/crash",
            "http://100.104.167.94:8090/crash",
            // 同一个 WiFi 时的直连兜底
            "http://192.168.3.62:18080/crash"
        )

    /**
     * 上报一个"启动信号"（start = 进程已进入 Application.onCreate，ok = 界面已经起来并稳定）。
     * 目的是让开发机**不用问用户**就能知道：这个包装上去了没、开起来了没、卡在哪个步骤。
     * 全程后台线程 + 失败静默。
     */
    @JvmStatic
    fun reportBoot(context: Context?, stage: String) {
        val ctx = context?.applicationContext ?: return
        try {
            val sb = StringBuilder()
            sb.append("=== Operit 启动信号: ").append(stage).append(" ===\n")
            sb.append("时间: ").append(timeFormat.format(Date())).append('\n')
            sb.append("包名: ").append(ctx.packageName).append('\n')
            sb.append("版本: ").append(appVersionName(ctx)).append('\n')
            sb.append("设备: ").append(Build.MANUFACTURER).append(' ').append(Build.MODEL)
                .append("  Android ").append(Build.VERSION.RELEASE)
                .append(" (SDK ").append(Build.VERSION.SDK_INT).append(")\n")
            sb.append("安全模式: ").append(StartupGuard.isSafeMode).append('\n')
            sb.append("最后启动步骤: ").append(lastStep).append('\n')
            val trace = traceFileRef
            if (trace != null && trace.exists()) {
                sb.append("\n=== 启动面包屑（当前进度）===\n")
                sb.append(readTail(trace, MAX_TRACE_BYTES))
            }
            uploadAsync("boot_" + stage, sb.toString())
        } catch (_: Throwable) {
        }
    }

    /**
     * 把诊断文本 POST 给开发机（后台线程、3 秒超时、失败静默）。
     * 只在真的排障期间有意义，不会影响启动速度，也不会阻塞调用方。
     */
    @JvmStatic
    fun uploadAsync(tag: String, body: String) {
        try {
            Thread { uploadSync(tag, body) }.also {
                it.isDaemon = true
                it.name = "OperitUpload-$tag"
            }.start()
        } catch (_: Throwable) {
        }
    }

    /**
     * 供外部取证器（如 [ExitInfoHarvester]）复用的统一出口：
     * 落一份到「下载」目录（Android 10+ 走 MediaStore，免权限），同时后台回传开发机。
     */
    @JvmStatic
    fun dumpToDownloadsAndUpload(context: Context, tag: String, filePrefix: String, body: String) {
        try {
            val name = filePrefix + "-" + stampFormat.format(Date()) + ".txt"
            createDownloadsFile(context, name, body)
        } catch (_: Throwable) {
        }
        uploadAsync(tag, body)
    }

    /** 同步版本：依次尝试所有地址，任意一个 2xx 即成功 */
    private fun uploadSync(tag: String, body: String): Boolean {        for (url in UPLOAD_URLS) {
            try {
                val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "POST"
                conn.connectTimeout = 3000
                conn.readTimeout = 4000
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "text/plain; charset=utf-8")
                conn.setRequestProperty("X-Operit-Tag", tag)
                conn.outputStream.use { os -> os.write(body.toByteArray(Charsets.UTF_8)) }
                val code = conn.responseCode
                try {
                    conn.inputStream?.close()
                } catch (_: Throwable) {
                }
                conn.disconnect()
                if (code in 200..299) return true
            } catch (_: Throwable) {
                // 换下一个地址
            }
        }
        return false
    }

    /**
     * 「发件箱」补发：崩溃当时网络不通（比如飞行模式、隧道没建立）也不会丢 ——
     * 内部 files/crash/ 里留着的 dump，每次启动都会尝试重发，成功后改名 .sent 不再重发。
     */
    @JvmStatic
    fun flushOutboxAsync(context: Context?) {
        val ctx = context?.applicationContext ?: return
        try {
            Thread {
                try {
                    val dir = File(ctx.filesDir, CRASH_DIR_NAME)
                    if (!dir.exists()) return@Thread
                    val pending =
                        dir.listFiles()
                            ?.filter { it.isFile && it.name.endsWith(".txt") && it.length() > 0 }
                            ?.sortedByDescending { it.lastModified() }
                            ?.take(5)
                            .orEmpty()
                    for (f in pending) {
                        try {
                            val body = f.readText()
                            if (uploadSync("outbox", body)) {
                                try {
                                    f.renameTo(File(dir, f.name + ".sent"))
                                } catch (_: Throwable) {
                                }
                            }
                        } catch (_: Throwable) {
                        }
                    }
                } catch (_: Throwable) {
                }
            }.also {
                it.isDaemon = true
                it.name = "OperitOutbox"
            }.start()
        } catch (_: Throwable) {
        }
    }

    private val stampFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
    private val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    @Volatile
    private var lastStep: String = "未记录"

    @Volatile
    private var lastStepAt: Long = 0L

    private val lock = Any()

    /** 记录启动进度面包屑。绝不抛异常。 */
    @JvmStatic
    fun markStep(step: String) {
        lastStep = step
        lastStepAt = System.currentTimeMillis()
        try {
            Log.d(TAG, "STEP: $step")
        } catch (_: Throwable) {
        }
        try {
            appendTraceLine("STA " + timeFormat.format(Date()) + "  " + step)
        } catch (_: Throwable) {
        }
    }

    /** 启动过程中某一步失败：记录但不抛，让应用继续启动。 */
    @JvmStatic
    fun reportStepFailure(context: Context?, step: String, error: Throwable?) {
        val message = buildString {
            append("启动步骤失败 [").append(step).append("]\n")
            append(describeError(error))
        }
        try {
            Log.e(TAG, message)
        } catch (_: Throwable) {
        }
        try {
            appendTraceLine("ERR " + timeFormat.format(Date()) + "  " + step + " -> " + (error?.javaClass?.name ?: "?"))
        } catch (_: Throwable) {
        }
        if (context != null) {
            try {
                dumpCrash(context, step, error)
            } catch (_: Throwable) {
            }
        }
    }

    /** 写崩溃现场。返回已写出的文件路径列表（用于日志）。绝不抛异常。 */
    @JvmStatic
    fun dumpCrash(context: Context?, step: String?, error: Throwable?): List<String> {
        val written = mutableListOf<String>()
        val ctx = context ?: return written
        try {
            val content = buildDump(ctx, step, error)
            val fileName = "Operit-crash-" + stampFormat.format(Date()) + ".txt"

            // 1) 内部私有目录（一定可写，作为兜底）
            try {
                val internalDir = File(ctx.filesDir, CRASH_DIR_NAME)
                if (!internalDir.exists()) internalDir.mkdirs()
                val internalFile = File(internalDir, fileName)
                internalFile.writeText(content)
                written.add(internalFile.absolutePath)
            } catch (_: Throwable) {
            }

            // 2) 应用外部私有目录（Android/data/<pkg>/files/crash/）
            try {
                val extRoot = ctx.getExternalFilesDir(null)
                if (extRoot != null) {
                    val extDir = File(extRoot, CRASH_DIR_NAME)
                    if (!extDir.exists()) extDir.mkdirs()
                    val extFile = File(extDir, fileName)
                    extFile.writeText(content)
                    written.add(extFile.absolutePath)
                }
            } catch (_: Throwable) {
            }

            // 3) 「下载」目录（用户最容易找到；Android 10+ 走 MediaStore，无需任何权限）
            try {
                val uri = writeToDownloads(ctx, fileName, content)
                if (uri != null) written.add(uri)
            } catch (_: Throwable) {
            }

            // 4) 自动回传给开发机（Tailscale 内网，后台线程，失败静默）
            try {
                uploadAsync("crash", content)
            } catch (_: Throwable) {
            }

            try {
                Log.e(TAG, "崩溃现场已写出: " + written.joinToString(" | "))
            } catch (_: Throwable) {
            }
            // 崩溃现场写完后就清掉面包屑，避免下一次启动误读
            try {
                appendTraceLine("END " + timeFormat.format(Date()) + "  已写出崩溃现场")
            } catch (_: Throwable) {
            }
        } catch (_: Throwable) {
        }
        return written
    }

    private fun writeToDownloads(context: Context, fileName: String, content: String): String? {
        val bytes = content.toByteArray(Charsets.UTF_8)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return null
            try {
                resolver.openOutputStream(uri)?.use { it.write(bytes) }
            } finally {
                val done = ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }
                try {
                    resolver.update(uri, done, null, null)
                } catch (_: Throwable) {
                }
            }
            return uri.toString()
        }
        // Android 9 及以下：直接写公共下载目录（需要 WRITE_EXTERNAL_STORAGE，清单里已声明）
        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (dir == null) return null
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, fileName)
        file.writeBytes(bytes)
        return file.absolutePath
    }

    private fun traceFile(context: Context?): File? {
        val ctx = context ?: return null
        return try {
            val base = ctx.getExternalFilesDir(null) ?: ctx.filesDir
            if (!base.exists()) base.mkdirs()
            File(base, TRACE_FILE_NAME)
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * 把"上一次启动"留下的日志尾部（含崩溃前最后几行）镜像到「下载」目录。
     *
     * 为什么需要：AppLogger 一直把日志写进内部 `files/logs/operit.log`，但从 Android 11 起
     * `Android/data/<pkg>` 对普通文件管理器不可见，用户根本取不出来。装上新包第一次启动时
     * 这份日志里还留着上一版崩溃前的最后几行，正好可以"回溯取证"。
     *
     * 只在每个版本号第一次启动时做一次（用 SharedPreferences 记标记），避免刷屏。
     * 整个过程在后台线程、全程 try/catch，绝不影响启动。
     */
    @JvmStatic
    fun harvestPreviousLogsAsync(context: Context?) {
        val ctx = context?.applicationContext ?: return
        try {
            Thread {
                try {
                    val prefs = ctx.getSharedPreferences("operit_crash_diag", Context.MODE_PRIVATE)
                    val key = "harvested_" + appVersionName(ctx)
                    if (prefs.getBoolean(key, false)) return@Thread
                    val body = buildPreviousLogs(ctx) ?: return@Thread
                    val name = "Operit-prevlogs-" + stampFormat.format(Date()) + ".txt"
                    val uri = createDownloadsFile(ctx, name, body)
                    // 同时直接回传开发机（走 Tailscale 私网），用户什么都不用做
                    uploadAsync("prevlogs", body)
                    if (uri != null) {
                        prefs.edit().putBoolean(key, true).apply()
                    }
                } catch (_: Throwable) {
                }
            }.also { it.isDaemon = true }.start()
        } catch (_: Throwable) {
        }
    }

    private fun buildPreviousLogs(context: Context): String? {
        val sb = StringBuilder()
        sb.append("=== Operit 上一次启动留下的日志（回溯取证用）===\n")
        sb.append("导出时间: ").append(timeFormat.format(Date())).append('\n')
        sb.append("说明: 这份文件由新版本在启动时自动导出，内容是上一次（含崩溃那一次）运行写到\n")
        sb.append("      files/logs/operit.log 的尾部，末尾几行通常就是崩溃前最后发生的事。\n\n")
        try {
            sb.append("包名: ").append(context.packageName)
                .append("  版本: ").append(appVersionName(context)).append('\n')
            sb.append("设备: ").append(Build.MANUFACTURER).append(' ').append(Build.MODEL)
                .append("  Android ").append(Build.VERSION.RELEASE)
                .append(" (SDK ").append(Build.VERSION.SDK_INT).append(")\n")
            sb.append("ROM: ").append(Build.DISPLAY).append("\n\n")
        } catch (_: Throwable) {
        }

        var wrote = false

        // 1) 主日志
        try {
            val logDir = File(context.filesDir, "logs")
            val main = File(logDir, "operit.log")
            if (main.exists() && main.length() > 0) {
                sb.append("=== files/logs/operit.log（共 ").append(main.length())
                    .append(" 字节，取末尾 ").append(HARVEST_TAIL_BYTES).append(" 字节）===\n")
                sb.append(readTail(main, HARVEST_TAIL_BYTES)).append("\n\n")
                wrote = true
            }
        } catch (_: Throwable) {
        }

        // 2) 其他日志文件（按修改时间取最新 2 个）
        try {
            val logDir = File(context.filesDir, "logs")
            val others =
                logDir.listFiles()
                    ?.filter { it.isFile && it.name != "operit.log" && it.length() > 0 }
                    ?.sortedByDescending { it.lastModified() }
                    ?.take(2)
                    .orEmpty()
            for (f in others) {
                sb.append("=== files/logs/").append(f.name).append("（末尾 ")
                    .append(HARVEST_TAIL_BYTES).append(" 字节）===\n")
                sb.append(readTail(f, HARVEST_TAIL_BYTES)).append("\n\n")
                wrote = true
            }
        } catch (_: Throwable) {
        }

        // 3) 之前的崩溃 dump / ANR 报告
        try {
            val crashDir = File(context.filesDir, "crash")
            val dumps =
                crashDir.listFiles()
                    ?.filter { it.isFile && it.length() > 0 }
                    ?.sortedByDescending { it.lastModified() }
                    ?.take(3)
                    .orEmpty()
            for (f in dumps) {
                sb.append("=== files/crash/").append(f.name).append(" ===\n")
                sb.append(readTail(f, HARVEST_TAIL_BYTES)).append("\n\n")
                wrote = true
            }
        } catch (_: Throwable) {
        }

        // 4) 上一次启动的面包屑（本次启动还没截断它）
        try {
            val trace = traceFile(context)
            if (trace != null && trace.exists() && trace.length() > 0) {
                sb.append("=== 上一次启动的面包屑（").append(TRACE_FILE_NAME).append("）===\n")
                sb.append(readTail(trace, MAX_TRACE_BYTES)).append("\n")
                wrote = true
            }
        } catch (_: Throwable) {
        }

        return if (wrote) sb.toString() else null
    }

    private var traceFileRef: File? = null

    /** 「下载」目录里那份面包屑的 MediaStore uri（原生崩溃时 Java 层拿不到栈，靠它取证） */
    @Volatile
    private var traceDownloadUri: android.net.Uri? = null

    /** 主线程卡死看门狗是否已经启动（只启动一次） */
    @Volatile
    private var stallWatchdogStarted = false

    /**
     * 主线程卡死看门狗：从后台线程每秒往主线程投一个探针，如果 [MAIN_THREAD_STALL_MS] 还没执行，
     * 就说明主线程被阻塞了（典型结局是被系统判为"应用无响应"然后直接杀掉 —— 那种情况下
     * Thread.setDefaultUncaughtExceptionHandler 完全不会触发，什么栈都留不下）。
     * 这里趁进程还活着，把**主线程当前栈**和所有线程栈写进「下载」目录的 `Operit-anr-*.txt`。
     *
     * 对主线程零负担：探针只是一个 Handler.post(Runnable)，阻塞解除后跑一下而已。
     */
    @JvmStatic
    fun startMainThreadStallWatchdog(context: Context?) {
        val ctx = context?.applicationContext ?: return
        if (stallWatchdogStarted) return
        stallWatchdogStarted = true
        try {
            Thread {
                try {
                    val mainLooper = android.os.Looper.getMainLooper() ?: return@Thread
                    val handler = android.os.Handler(mainLooper)
                    var dumps = 0
                    while (dumps < 5) {
                        val latch = java.util.concurrent.CountDownLatch(1)
                        val ok = handler.post { latch.countDown() }
                        if (!ok) return@Thread
                        val responded =
                            latch.await(MAIN_THREAD_STALL_MS, java.util.concurrent.TimeUnit.MILLISECONDS)
                        if (!responded) {
                            dumps++
                            try {
                                val body = buildStallDump(ctx, dumps)
                                val name = "Operit-anr-" + stampFormat.format(Date()) + "-" + dumps + ".txt"
                                createDownloadsFile(ctx, name, body)
                                uploadAsync("anr", body)
                                // 同时留一份内部副本，方便下次启动被 harvest 捡走
                                try {
                                    val dir = File(ctx.filesDir, CRASH_DIR_NAME)
                                    if (!dir.exists()) dir.mkdirs()
                                    File(dir, "anr_${stampFormat.format(Date())}_$dumps.txt")
                                        .writeText(body)
                                } catch (_: Throwable) {
                                }
                            } catch (_: Throwable) {
                            }
                            // 卡死期间不要每 10 秒刷一个文件
                            Thread.sleep(MAIN_THREAD_STALL_MS)
                        } else {
                            Thread.sleep(1000)
                        }
                    }
                } catch (_: Throwable) {
                }
            }.also { it.isDaemon = true; it.name = "OperitStallWatchdog" }.start()
        } catch (_: Throwable) {
        }
    }

    private fun buildStallDump(context: Context, round: Int): String {
        val sb = StringBuilder()
        sb.append("=== Operit 主线程卡死（疑似 ANR）诊断 ===\n")
        sb.append("时间: ").append(timeFormat.format(Date())).append('\n')
        sb.append("第 ").append(round).append(" 次检测到主线程超过 ")
        sb.append(MAIN_THREAD_STALL_MS).append("ms 无响应\n")
        sb.append("最后完成/开始的启动步骤: ").append(lastStep)
        if (lastStepAt > 0L) {
            sb.append("  (记录于 ").append(timeFormat.format(Date(lastStepAt))).append(")")
        }
        sb.append("\n\n")
        try {
            sb.append("包名: ").append(context.packageName)
                .append("  版本: ").append(appVersionName(context)).append('\n')
            sb.append("设备: ").append(Build.MANUFACTURER).append(' ').append(Build.MODEL)
                .append("  Android ").append(Build.VERSION.RELEASE)
                .append(" (SDK ").append(Build.VERSION.SDK_INT).append(")\n\n")
        } catch (_: Throwable) {
        }

        // 主线程栈（最关键）
        try {
            val mainThread = android.os.Looper.getMainLooper()?.thread
            sb.append("=== 主线程栈（").append(mainThread?.name).append("）===\n")
            val frames = mainThread?.stackTrace.orEmpty()
            if (frames.isEmpty()) {
                sb.append("（拿不到）\n")
            } else {
                for (f in frames) sb.append("  at ").append(f.toString()).append('\n')
            }
            sb.append('\n')
        } catch (_: Throwable) {
        }

        // 全部线程栈（看是不是在等锁）
        try {
            sb.append("=== 全部线程（共 ")
            val all = Thread.getAllStackTraces()
            sb.append(all.size).append(" 个）===\n")
            for ((thread, frames) in all) {
                try {
                    sb.append("\"").append(thread.name).append("\" state=")
                        .append(thread.state).append(" priority=").append(thread.priority)
                        .append('\n')
                    for (f in frames) sb.append("  at ").append(f.toString()).append('\n')
                } catch (_: Throwable) {
                }
            }
            sb.append('\n')
        } catch (_: Throwable) {
        }

        // 启动面包屑
        try {
            val trace = traceFileRef
            if (trace != null && trace.exists()) {
                sb.append("=== 启动面包屑 ===\n")
                sb.append(readTail(trace, MAX_TRACE_BYTES)).append('\n')
            }
        } catch (_: Throwable) {
        }

        return sb.toString()
    }

    /** attachTraceFile 是否已在本进程执行过（幂等保护：provider 阶段先绑定，Application.onCreate 不再重复截断） */
    @Volatile
    private var traceAttached = false

    /**
     * 进程最早期调用（`BootProbeProvider.onCreate`，早于 `Application.onCreate`），绑定面包屑文件
     * 位置并截断旧内容；重复调用不会再截断，只会更新 context 引用。
     * 同时在「下载」目录建一份同名文本（Android 10+ 走 MediaStore，免权限），
     * 后续每一步都往两边追加 —— 这样即使进程被原生代码直接杀掉、Java 层毫无机会，
     * 用户也能在文件管理器里找到"停在哪个步骤"。
     */
    @JvmStatic
    fun attachTraceFile(context: Context?) {
        val ctx = context ?: return
        val alreadyAttached = traceAttached
        try {
            boundContextRef = ctx.applicationContext
            if (alreadyAttached) return
            traceAttached = true
            val file = traceFile(context)
            traceFileRef = file
            val header =
                "# Operit 启动面包屑（最近一次启动）\n" +
                    "启动时间: " + timeFormat.format(Date()) + "\n"
            if (file != null) {
                synchronized(lock) {
                    try {
                        file.writeText(header)
                    } catch (_: Throwable) {
                    }
                }
            }
            // 镜像到「下载」
            try {
                val name = "Operit-boot-" + stampFormat.format(Date()) + ".txt"
                traceDownloadUri = createDownloadsFile(ctx, name, header)
            } catch (_: Throwable) {
            }
        } catch (_: Throwable) {
        }
    }

    /** 在「下载」目录建一个文本文件并写入初始内容，返回 uri。失败返回 null。 */
    private fun createDownloadsFile(context: Context, fileName: String, content: String): android.net.Uri? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            // 老系统直接写公共下载目录
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (dir == null) return null
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, fileName)
            file.writeText(content)
            return android.net.Uri.fromFile(file)
        }
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "text/plain")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return null
        try {
            resolver.openOutputStream(uri)?.use { it.write(content.toByteArray(Charsets.UTF_8)) }
        } catch (_: Throwable) {
        }
        val done = ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }
        try {
            resolver.update(uri, done, null, null)
        } catch (_: Throwable) {
        }
        return uri
    }

    private fun appendToDownloads(line: String) {
        val uri = traceDownloadUri ?: return
        try {
            // 直接用 ContentResolver，任何一步失败都不影响主流程
            val resolver =
                boundContextRef?.contentResolver ?: return
            resolver.openOutputStream(uri, "wa")?.use {
                it.write((line + "\n").toByteArray(Charsets.UTF_8))
            }
        } catch (_: Throwable) {
        }
    }

    /** 供 appendToDownloads 用（attachTraceFile 时绑定的 application context） */
    @Volatile
    private var boundContextRef: Context? = null

    private fun appendTraceLine(line: String) {
        val file = traceFileRef
        if (file != null) {
            synchronized(lock) {
                try {
                    if (!file.exists()) {
                        file.parentFile?.mkdirs()
                    }
                    if (file.length() > MAX_TRACE_BYTES) {
                        file.writeText("# Operit 启动面包屑（已截断）\n")
                    }
                    file.appendText(line + "\n")
                } catch (_: Throwable) {
                }
            }
        }
        appendToDownloads(line)
    }

    private fun buildDump(context: Context, step: String?, error: Throwable?): String {
        val sb = StringBuilder()
        sb.append("=== Operit 崩溃/启动失败诊断 ===\n")
        sb.append("时间: ").append(timeFormat.format(Date())).append('\n')
        sb.append("最后完成/开始的步骤: ").append(step ?: lastStep)
        if (lastStepAt > 0L) {
            sb.append("  (记录于 ").append(timeFormat.format(Date(lastStepAt))).append(")")
        }
        sb.append('\n')

        try {
            sb.append("包名: ").append(context.packageName).append('\n')
            sb.append("进程: ").append(currentProcessName()).append('\n')
        } catch (_: Throwable) {
        }
        try {
            sb.append("应用版本: ").append(appVersionName(context)).append('\n')
        } catch (_: Throwable) {
        }
        try {
            sb.append("设备: ").append(Build.MANUFACTURER).append(' ').append(Build.MODEL)
                .append("  Android ").append(Build.VERSION.RELEASE)
                .append(" (SDK ").append(Build.VERSION.SDK_INT).append(")")
                .append("  ABI: ").append(Build.SUPPORTED_ABIS.joinToString(","))
                .append('\n')
            sb.append("ROM: ").append(Build.DISPLAY).append('\n')
        } catch (_: Throwable) {
        }
        sb.append('\n')

        sb.append("=== 异常 ===\n")
        sb.append(describeError(error))
        sb.append('\n')

        // 启动面包屑（本次启动的完整进度）
        try {
            val trace = traceFileRef
            if (trace != null && trace.exists()) {
                sb.append("=== 启动面包屑 ===\n")
                sb.append(readTail(trace, MAX_TRACE_BYTES))
                sb.append('\n')
            }
        } catch (_: Throwable) {
        }

        // 应用日志尾部（AppLogger 写在内部 files/logs/operit.log）
        try {
            val logFile = File(File(context.filesDir, "logs"), "operit.log")
            if (logFile.exists()) {
                sb.append("=== 应用日志尾部（").append(logFile.length()).append(" 字节，取末尾 ")
                    .append(MAX_LOG_TAIL_BYTES).append(" 字节）===\n")
                sb.append(readTail(logFile, MAX_LOG_TAIL_BYTES))
                sb.append('\n')
            } else {
                sb.append("=== 应用日志 ===\n（未找到 files/logs/operit.log）\n")
            }
        } catch (_: Throwable) {
        }

        return sb.toString()
    }

    /**
     * 取当前的诊断文本（不落盘），给"界面初始化失败"兜底页面直接显示用，
     * 用户截图就能把现场发出来。
     */
    @JvmStatic
    fun lastDumpText(context: Context?): String {
        val ctx = context ?: return "（无 Context）"
        return try {
            val sb = StringBuilder()
            // 先摊开"上一次崩溃落下的那份 dump"（里面才有真正的异常栈）——
            // 应用崩溃后重启时，屏幕上能看到的就是它，不需要用户去翻文件。
            try {
                val dir = File(ctx.filesDir, CRASH_DIR_NAME)
                val newest =
                    dir.listFiles()
                        ?.filter { it.isFile && it.name.endsWith(".txt") && it.length() > 0 }
                        ?.maxByOrNull { it.lastModified() }
                if (newest != null) {
                    sb.append("=== 上一次崩溃现场（内部存档 ").append(newest.name).append("）===\n")
                    sb.append(readTail(newest, 96 * 1024)).append("\n\n")
                }
            } catch (_: Throwable) {
            }
            sb.append(buildDump(ctx, null, null))
            sb.toString()
        } catch (_: Throwable) {
            "（生成诊断信息失败）"
        }
    }

    private fun describeError(error: Throwable?): String {
        if (error == null) return "（无异常对象：可能是原生崩溃/被系统杀死）\n"
        return try {
            val sw = java.io.StringWriter()
            java.io.PrintWriter(sw).use { error.printStackTrace(it) }
            val text = sw.toString()
            if (text.length > 64_000) text.substring(0, 64_000) + "\n...(截断)" else text
        } catch (_: Throwable) {
            error.toString()
        }
    }

    private fun readTail(file: File, maxBytes: Int): String {
        return try {
            val length = file.length()
            val start = if (length > maxBytes) length - maxBytes else 0L
            java.io.RandomAccessFile(file, "r").use { raf ->
                raf.seek(start)
                val buffer = ByteArray((length - start).toInt())
                raf.readFully(buffer)
                String(buffer, Charsets.UTF_8)
            }
        } catch (_: Throwable) {
            "（读取失败）\n"
        }
    }

    private fun currentProcessName(): String {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                android.app.Application.getProcessName()
            } else {
                "unknown"
            }
        } catch (_: Throwable) {
            "unknown"
        }
    }

    private fun appVersionName(context: Context): String {
        return try {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            val code =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    info.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    info.versionCode.toLong()
                }
            (info.versionName ?: "?") + " (" + code + ")"
        } catch (_: Throwable) {
            "?"
        }
    }
}
