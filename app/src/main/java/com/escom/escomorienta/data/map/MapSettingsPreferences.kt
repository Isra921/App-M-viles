package com.escom.escomorienta.data.map

import android.content.Context

/**
 * Gestor de persistencia ligera para preferencias del módulo de mapa y accesibilidad.
 */
object MapSettingsPreferences {
    private const val PREFS_NAME = "escom_map_settings"
    private const val KEY_PREFER_ACCESSIBLE = "prefer_accessible_routes"

    fun isAccessibleRoutePreferred(context: Context): Boolean {
        // Desactivado temporalmente para concentrar el sistema en la navegación convencional.
        // Se ignora cualquier valor previo almacenado para evitar que altere las rutas estándar.
        return false
    }

    fun setAccessibleRoutePreferred(context: Context, preferred: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_PREFER_ACCESSIBLE, preferred)
            .apply()
    }
}
