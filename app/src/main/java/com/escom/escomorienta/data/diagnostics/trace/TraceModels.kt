package com.escom.escomorienta.data.diagnostics.trace

enum class TraceModule {
    APP, ACTION, 
    SCHEDULE, MAP, ROUTE, NOTIFICATION, SETTINGS, PERSISTENCE, 
    UPDATE, UI
}

enum class TraceStatus {
    STARTED, SUCCESS, FAILURE, SKIPPED, FALLBACK, CANCELLED
}

data class TraceEvent(
    val traceId: String,
    val operationId: String? = null,
    val sequence: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val module: TraceModule,
    val stage: String,
    val eventType: String,
    val status: TraceStatus,
    val durationMs: Long? = null,
    val attributes: Map<String, String> = emptyMap(),
    val errorCategory: String? = null,
    val source: String? = null
)

interface ExecutionTracer {
    fun record(event: TraceEvent)
    fun currentTraceId(): String?
    fun startTrace(operationId: String? = null): String?
}
