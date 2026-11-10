package com.escom.escomorienta.data.settings

import java.text.Normalizer

fun subjectSelectionKey(subject: String): String =
    Normalizer.normalize(subject, Normalizer.Form.NFD)
        .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
        .lowercase()
        .replace("[^a-z0-9]+".toRegex(), "_")
        .trim('_')

fun decodeSubjectSelection(value: String): Set<String> = value
    .split(';')
    .map(String::trim)
    .filter(String::isNotBlank)
    .toSet()

fun encodeSubjectSelection(values: Set<String>): String =
    values.filter(String::isNotBlank).sorted().joinToString(";")
