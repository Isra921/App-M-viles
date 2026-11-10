package com.escom.escomorienta.domain.notifications

enum class ReminderProfile(val displayName: String) {
    RECOMMENDED("Recomendado"),
    MODERATE("Moderado"),
    INTENSIVE("Intensivo"),
    CUSTOM("Personalizado")
}

data class ReminderScheme(
    val profile: ReminderProfile,
    val count: Int,
    val intervalMinutes: Int,
    val offsetsMinutes: List<Int>
) {
    val furthestTriggerMinutes: Int
        get() = offsetsMinutes.maxOrNull() ?: 0

    fun encode(): Int = when (profile) {
        ReminderProfile.RECOMMENDED -> 101
        ReminderProfile.MODERATE -> 201
        ReminderProfile.INTENSIVE -> 301
        ReminderProfile.CUSTOM -> 1000 + count * 100 + intervalMinutes
    }

    companion object {
        const val MAX_ADVANCE_MINUTES = 60
        const val MIN_INTERVAL_MINUTES = 2
        const val MAX_INTERVAL_MINUTES = 15
        const val MIN_COUNT = 1
        const val MAX_COUNT = 5

        val RECOMMENDED = ReminderScheme(
            profile = ReminderProfile.RECOMMENDED,
            count = 2,
            intervalMinutes = 15,
            offsetsMinutes = listOf(30, 15)
        )

        val MODERATE = ReminderScheme(
            profile = ReminderProfile.MODERATE,
            count = 2,
            intervalMinutes = 10,
            offsetsMinutes = listOf(20, 10)
        )

        val INTENSIVE = ReminderScheme(
            profile = ReminderProfile.INTENSIVE,
            count = 5,
            intervalMinutes = 2,
            offsetsMinutes = listOf(10, 8, 6, 4, 2)
        )

        fun custom(count: Int, intervalMinutes: Int): ReminderScheme? {
            val c = count.coerceIn(MIN_COUNT, MAX_COUNT)
            val i = intervalMinutes.coerceIn(MIN_INTERVAL_MINUTES, MAX_INTERVAL_MINUTES)
            val furthest = c * i
            if (furthest > MAX_ADVANCE_MINUTES) return null
            val offsets = (c downTo 1).map { it * i }
            return ReminderScheme(ReminderProfile.CUSTOM, c, i, offsets)
        }

        fun fromAdvanceMinutes(minutes: Int): ReminderScheme? {
            if (minutes <= 0 || minutes > MAX_ADVANCE_MINUTES) return null
            return when (minutes) {
                10 -> MODERATE
                15 -> RECOMMENDED
                20 -> ReminderScheme(ReminderProfile.CUSTOM, 1, 20, listOf(20))
                30 -> ReminderScheme(ReminderProfile.CUSTOM, 1, 30, listOf(30))
                45 -> ReminderScheme(ReminderProfile.CUSTOM, 1, 45, listOf(45))
                60 -> ReminderScheme(ReminderProfile.CUSTOM, 1, 60, listOf(60))
                else -> ReminderScheme(ReminderProfile.CUSTOM, 1, minutes, listOf(minutes))
            }
        }

        fun fromEncodedValue(encoded: Int): ReminderScheme {
            return when {
                encoded == 101 -> RECOMMENDED
                encoded == 201 -> MODERATE
                encoded == 301 -> INTENSIVE
                encoded in 1..MAX_ADVANCE_MINUTES -> fromAdvanceMinutes(encoded) ?: MODERATE
                encoded >= 1102 -> {
                    val count = ((encoded - 1000) / 100).coerceIn(MIN_COUNT, MAX_COUNT)
                    val interval = ((encoded - 1000) % 100).coerceIn(MIN_INTERVAL_MINUTES, MAX_INTERVAL_MINUTES)
                    val offsets = (count downTo 1).map { it * interval }
                    ReminderScheme(ReminderProfile.CUSTOM, count, interval, offsets)
                }
                else -> RECOMMENDED
            }
        }

        fun validateOffsets(offsets: List<Int>): Boolean {
            if (offsets.isEmpty()) return false
            return offsets.all { it in 1..MAX_ADVANCE_MINUTES }
        }
    }
}
