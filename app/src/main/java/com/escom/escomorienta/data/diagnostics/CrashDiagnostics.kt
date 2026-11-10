package com.escom.escomorienta.data.diagnostics

import android.content.Context
import android.os.Build
import androidx.core.content.FileProvider
import com.escom.escomorienta.BuildConfig
import org.json.JSONObject
import java.io.File
import java.time.Instant
import java.util.concurrent.atomic.AtomicReference

data class CrashDiagnosticReport(
    val schemaVersion: Int = 1,
    val appVersion: String,
    val versionCode: Int,
    val buildCommit: String,
    val buildType: String,
    val androidVersion: String,
    val apiLevel: Int,
    val manufacturer: String,
    val model: String,
    val abis: List<String>,
    val exceptionClass: String,
    val stackTrace: String,
    val thread: String,
    val timestampUtc: String,
    val lifecycleStage: String
)

object CrashLifecycleStage {
    private val current = AtomicReference("NOT_SET")
    fun update(stage: String) { current.set(stage.take(MAX_STAGE_LENGTH)) }
    fun current(): String = current.get()
    private const val MAX_STAGE_LENGTH = 64
}

object CrashDiagnosticCodec {
    const val MAX_STACK_CHARS = 16_384

    fun encode(report: CrashDiagnosticReport): String = JSONObject().apply {
        put("schemaVersion", report.schemaVersion)
        put("appVersion", report.appVersion)
        put("versionCode", report.versionCode)
        put("buildCommit", report.buildCommit)
        put("buildType", report.buildType)
        put("androidVersion", report.androidVersion)
        put("apiLevel", report.apiLevel)
        put("manufacturer", report.manufacturer)
        put("model", report.model)
        put("abis", org.json.JSONArray(report.abis))
        put("exceptionClass", report.exceptionClass)
        put("stackTrace", report.stackTrace.take(MAX_STACK_CHARS))
        put("thread", report.thread)
        put("timestampUtc", report.timestampUtc)
        put("lifecycleStage", report.lifecycleStage)
    }.toString()

    fun decode(value: String): CrashDiagnosticReport {
        val json = JSONObject(value)
        val abis = json.getJSONArray("abis")
        return CrashDiagnosticReport(
            schemaVersion = json.optInt("schemaVersion", 1),
            appVersion = json.getString("appVersion"), versionCode = json.getInt("versionCode"),
            buildCommit = json.getString("buildCommit"), buildType = json.getString("buildType"),
            androidVersion = json.getString("androidVersion"), apiLevel = json.getInt("apiLevel"),
            manufacturer = json.getString("manufacturer"), model = json.getString("model"),
            abis = (0 until abis.length()).map { abis.getString(it) },
            exceptionClass = json.getString("exceptionClass"),
            stackTrace = json.getString("stackTrace").take(MAX_STACK_CHARS),
            thread = json.getString("thread"), timestampUtc = json.getString("timestampUtc"),
            lifecycleStage = json.getString("lifecycleStage")
        )
    }
}

class CrashDiagnosticStore(private val filesDirectory: File) {
    val reportFile: File get() = File(filesDirectory, "diagnostics/last_crash.json")

    fun write(report: CrashDiagnosticReport): Boolean = runCatching {
        val target = reportFile
        target.parentFile?.mkdirs()
        val temporary = File(target.parentFile, "last_crash.tmp")
        temporary.writeText(CrashDiagnosticCodec.encode(report), Charsets.UTF_8)
        if (!temporary.renameTo(target)) {
            target.writeText(temporary.readText(Charsets.UTF_8), Charsets.UTF_8)
            temporary.delete()
        }
        true
    }.getOrDefault(false)

    fun read(): CrashDiagnosticReport? = runCatching {
        reportFile.takeIf(File::isFile)?.readText(Charsets.UTF_8)?.let(CrashDiagnosticCodec::decode)
    }.getOrNull()

    fun delete(): Boolean = !reportFile.exists() || runCatching { reportFile.delete() }.getOrDefault(false)
}

class LocalCrashExceptionHandler(
    private val previous: Thread.UncaughtExceptionHandler?,
    private val reportWriter: (CrashDiagnosticReport) -> Unit,
    private val reportFactory: (Thread, Throwable) -> CrashDiagnosticReport
) : Thread.UncaughtExceptionHandler {
    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        if (throwable !is OutOfMemoryError) runCatching { reportWriter(reportFactory(thread, throwable)) }
        previous?.uncaughtException(thread, throwable)
    }
}

object CrashDiagnosticManager {
    @Volatile private var installed = false

    fun install(context: Context) {
        if (installed) return
        synchronized(this) {
            if (installed) return
            val app = context.applicationContext
            val store = CrashDiagnosticStore(app.filesDir)
            val previous = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler(LocalCrashExceptionHandler(previous, { store.write(it) }) { thread, error ->
                createReport(thread, error)
            })
            installed = true
        }
    }

    fun store(context: Context) = CrashDiagnosticStore(context.applicationContext.filesDir)

    fun shareUri(context: Context) = FileProvider.getUriForFile(
        context, "${context.packageName}.fileprovider", store(context).reportFile
    )

    private fun createReport(thread: Thread, error: Throwable): CrashDiagnosticReport = CrashDiagnosticReport(
        appVersion = BuildConfig.VERSION_NAME,
        versionCode = BuildConfig.VERSION_CODE,
        buildCommit = BuildConfig.BUILD_COMMIT,
        buildType = BuildConfig.BUILD_TYPE,
        androidVersion = Build.VERSION.RELEASE,
        apiLevel = Build.VERSION.SDK_INT,
        manufacturer = Build.MANUFACTURER.take(80),
        model = Build.MODEL.take(80),
        abis = Build.SUPPORTED_ABIS.take(8),
        exceptionClass = error::class.java.name.take(200),
        stackTrace = boundedStack(error),
        thread = thread.name.take(100),
        timestampUtc = Instant.now().toString(),
        lifecycleStage = CrashLifecycleStage.current()
    )

    internal fun boundedStack(error: Throwable): String = buildString {
        var current: Throwable? = error
        var depth = 0
        while (current != null && depth < 4 && length < CrashDiagnosticCodec.MAX_STACK_CHARS) {
            if (depth > 0) append("Caused by: ")
            append(current::class.java.name).append('\n')
            current.stackTrace.take(80).forEach { append("  at ").append(it).append('\n') }
            current = current.cause
            depth++
        }
    }.take(CrashDiagnosticCodec.MAX_STACK_CHARS)
}
