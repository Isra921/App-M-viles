package com.escom.escomorienta.data.schedule.pdf

data class PdfTextDocument(
    val plainText: String,
    val tokens: List<PdfTextToken>
)

data class PdfTextToken(
    val text: String,
    val pageIndex: Int,
    val x: Float,
    val y: Float,
    val endX: Float,
    val height: Float
) {
    val centerX: Float = (x + endX) / 2f
}

data class PdfTextLine(
    val text: String,
    val pageIndex: Int,
    val x: Float,
    val y: Float,
    val endX: Float
)
