package com.escom.escomorienta.data.schedule

fun normalizeKnownClassroom(value: String?): String? {
    val trimmed = value?.trim().orEmpty()
    if (!trimmed.matches(NumericClassroomRegex)) return null
    return trimmed.takeIf { isKnownClassroom(it) }
}

fun isKnownClassroom(value: String?): Boolean {
    val trimmed = value?.trim() ?: return false
    val number = trimmed.toIntOrNull() ?: return false
    return trimmed.matches(NumericClassroomRegex) &&
        AllowedClassroomRanges.any { range -> number in range }
}

val AllowedClassroomRanges: List<IntRange> = listOf(
    1001..1014,
    1101..1114,
    1201..1214,
    2001..2014,
    2101..2114,
    2201..2214,
    3003..3014,
    3101..3114,
    3201..3214,
    4001..4014,
    4101..4114,
    4201..4213
)

private val NumericClassroomRegex = Regex("""\d{4}""")
