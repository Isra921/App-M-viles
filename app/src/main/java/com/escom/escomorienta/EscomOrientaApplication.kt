package com.escom.escomorienta

import android.app.Application
import com.escom.escomorienta.data.diagnostics.CrashDiagnosticManager
import com.escom.escomorienta.data.diagnostics.CrashLifecycleStage

class EscomOrientaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashLifecycleStage.update("APP_START")
        CrashDiagnosticManager.install(this)
    }
}
