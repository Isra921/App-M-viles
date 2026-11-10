package com.escom.escomorienta.data.diagnostics.trace

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.io.File
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.ConcurrentLinkedQueue

class StructuredExecutionTracer(
    private val context: Context,
    private val mode: TraceMode = TraceMode.BASIC
) : ExecutionTracer {
    
    private var activeTraceId: String? = null
    private val sequence = AtomicLong(0)
    private val currentSessionEvents = ConcurrentLinkedQueue<TraceEvent>()
    
    enum class TraceMode { OFF, BASIC, DIAGNOSTIC }

    override fun record(event: TraceEvent) {
        if (mode == TraceMode.OFF) return
        
        currentSessionEvents.add(event)
        
        if (mode == TraceMode.DIAGNOSTIC) {
            Log.d("ExecutionTrace", "[${event.module}] ${event.stage}: ${event.status} (${event.attributes})")
        }
        
        // Auto-save on critical events or periodically
        if (event.status == TraceStatus.FAILURE || currentSessionEvents.size >= 50) {
            persist()
        }
    }

    override fun currentTraceId(): String? = activeTraceId

    override fun startTrace(operationId: String?): String {
        activeTraceId = UUID.randomUUID().toString()
        val id = activeTraceId!!
        sequence.set(0)
        record(TraceEvent(
            traceId = id,
            operationId = operationId,
            sequence = sequence.getAndIncrement(),
            module = TraceModule.APP,
            stage = "TRACE_START",
            eventType = "LIFECYCLE",
            status = TraceStatus.STARTED
        ))
        return id
    }

    fun persist() {
        if (currentSessionEvents.isEmpty()) return
        
        val eventsToPersist = mutableListOf<TraceEvent>()
        while (currentSessionEvents.isNotEmpty()) {
            currentSessionEvents.poll()?.let { eventsToPersist.add(it) }
        }
        
        if (eventsToPersist.isEmpty()) return

        Log.i("ExecutionTrace", "Persisting ${eventsToPersist.size} events")
        val traceFile = File(context.filesDir, "traces/execution_traces.jsonl")
        traceFile.parentFile?.mkdirs()
        
        try {
            traceFile.appendText(eventsToPersist.joinToString("\n") { it.toJson().toString() } + "\n")
            // Policy: Keep file under 2MB
            if (traceFile.length() > 2 * 1024 * 1024) {
                rotate(traceFile)
            }
        } catch (e: Exception) {
            Log.e("ExecutionTrace", "Failed to persist traces", e)
        }
    }

    private fun rotate(file: File) {
        val backup = File(file.absolutePath + ".old")
        if (backup.exists()) backup.delete()
        file.renameTo(backup)
    }

    private fun TraceEvent.toJson() = JSONObject().apply {
        put("traceId", traceId)
        put("operationId", operationId ?: JSONObject.NULL)
        put("seq", sequence)
        put("ts", timestamp)
        put("mod", module.name)
        put("stg", stage)
        put("type", eventType)
        put("status", status.name)
        put("dur", durationMs ?: JSONObject.NULL)
        put("attr", JSONObject(attributes))
        put("err", errorCategory ?: JSONObject.NULL)
        put("src", source ?: JSONObject.NULL)
    }
}

class NoOpExecutionTracer : ExecutionTracer {
    override fun record(event: TraceEvent) {}
    override fun currentTraceId(): String? = null
    override fun startTrace(operationId: String?): String? = null
}
