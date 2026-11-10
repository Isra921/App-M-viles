package com.escom.escomorienta.data.settings

enum class SchedulePaletteMode {
    INSTITUTIONAL,
    PASTEL,
    WARM,
    ACCESSIBLE,
    COLD,
    CUSTOM;

    companion object {
        fun fromStoredValue(value: String): SchedulePaletteMode {
            return entries.firstOrNull { it.name == value } ?: INSTITUTIONAL
        }
    }
}
