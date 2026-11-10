package com.escom.escomorienta.data.settings

enum class NotificationScope {
    ALL_SUBJECTS,
    SELECTED_SUBJECTS;

    companion object {
        fun fromStoredValue(value: String): NotificationScope {
            return entries.firstOrNull { it.name == value } ?: ALL_SUBJECTS
        }
    }
}
