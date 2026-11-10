package com.escom.escomorienta.data.diagnostics

import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files

class CrashDiagnosticsTest {
    @Test fun `report serializes and restores structured technical fields`() {
        assertEquals(report(), CrashDiagnosticCodec.decode(CrashDiagnosticCodec.encode(report())))
    }

    @Test fun `missing and corrupt files return null`() {
        val root = Files.createTempDirectory("crash-store").toFile()
        val store = CrashDiagnosticStore(root)
        assertNull(store.read())
        store.reportFile.parentFile.mkdirs()
        store.reportFile.writeText("{")
        assertNull(store.read())
    }

    @Test fun `write exposes only the diagnostic file and delete removes it`() {
        val root = Files.createTempDirectory("crash-store").toFile()
        val store = CrashDiagnosticStore(root)
        assertTrue(store.write(report()))
        assertEquals("last_crash.json", store.reportFile.name)
        assertEquals(report(), store.read())
        assertTrue(store.delete())
        assertFalse(store.reportFile.exists())
    }

    @Test fun `stack is truncated and sensitive fields do not exist`() {
        val encoded = CrashDiagnosticCodec.encode(report().copy(stackTrace = "x".repeat(40_000)))
        val restored = CrashDiagnosticCodec.decode(encoded)
        assertEquals(CrashDiagnosticCodec.MAX_STACK_CHARS, restored.stackTrace.length)
        listOf("query", "boleta", "email", "alias", "gps", "androidId", "imei", "advertisingId").forEach {
            assertFalse("Unexpected sensitive key: $it", encoded.contains("\"$it\"", ignoreCase = true))
        }
    }

    @Test fun `handler delegates to previous handler after best effort write`() {
        var written = false
        var delegated = false
        val previous = Thread.UncaughtExceptionHandler { _, _ -> delegated = true }
        val handler = LocalCrashExceptionHandler(previous, { written = true }) { _, _ -> report() }
        handler.uncaughtException(Thread.currentThread(), IllegalStateException("private input"))
        assertTrue(written)
        assertTrue(delegated)
    }

    @Test fun `handler still delegates when writer fails and avoids heavy OOM report`() {
        var factoryCalls = 0
        var delegated = 0
        val handler = LocalCrashExceptionHandler(
            Thread.UncaughtExceptionHandler { _, _ -> delegated++ },
            { error("disk unavailable") },
            { _, _ -> factoryCalls++; report() }
        )
        handler.uncaughtException(Thread.currentThread(), IllegalArgumentException())
        handler.uncaughtException(Thread.currentThread(), OutOfMemoryError())
        assertEquals(1, factoryCalls)
        assertEquals(2, delegated)
    }

    private fun report() = CrashDiagnosticReport(
        appVersion = "1.0.1-step11.2", versionCode = 2, buildCommit = "abcdef1", buildType = "debug",
        androidVersion = "15", apiLevel = 35, manufacturer = "maker", model = "model",
        abis = listOf("arm64-v8a"), exceptionClass = "java.lang.IllegalStateException",
        stackTrace = "java.lang.IllegalStateException\n  at example.Test.run(Test.kt:1)\n", thread = "main",
        timestampUtc = "2026-08-24T00:00:00Z", lifecycleStage = "SESSION_RESTORE"
    )
}
