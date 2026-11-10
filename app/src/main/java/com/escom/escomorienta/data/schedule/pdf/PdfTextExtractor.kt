package com.escom.escomorienta.data.schedule.pdf

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition

class PdfTextExtractor(
    private val context: Context
) {
    fun extract(uri: Uri): PdfTextDocument {
        PDFBoxResourceLoader.init(context.applicationContext)

        context.contentResolver.openInputStream(uri).use { inputStream ->
            requireNotNull(inputStream) { "No se pudo abrir el archivo seleccionado." }

            PDDocument.load(inputStream).use { document ->
                val text = StringBuilder()
                val tokens = mutableListOf<PdfTextToken>()

                for (page in 1..document.numberOfPages) {
                    val stripper = PositionedTextStripper(pageIndex = page - 1)
                    stripper.startPage = page
                    stripper.endPage = page
                    text.append(stripper.getText(document))
                    text.append('\n')
                    tokens += stripper.tokens
                }

                return PdfTextDocument(
                    plainText = text.toString(),
                    tokens = tokens
                )
            }
        }
    }
}

internal class PositionedTextStripper(
    private val pageIndex: Int
) : PDFTextStripper() {
    val tokens = mutableListOf<PdfTextToken>()

    init {
        setSortByPosition(true)
    }

    override fun writeString(text: String, textPositions: MutableList<TextPosition>) {
        collectWordTokens(textPositions)
        super.writeString(text, textPositions)
    }

    private fun collectWordTokens(textPositions: List<TextPosition>) {
        val current = mutableListOf<TextPosition>()

        fun flush() {
            if (current.isEmpty()) return
            val word = current.joinToString(separator = "") { it.unicode }.trim()
            if (word.isNotBlank()) {
                tokens += PdfTextToken(
                    text = word,
                    pageIndex = pageIndex,
                    x = current.minOf { it.xDirAdj },
                    y = current.map { it.yDirAdj }.average().toFloat(),
                    endX = current.maxOf { it.xDirAdj + it.widthDirAdj },
                    height = current.maxOf { it.heightDir }
                )
            }
            current.clear()
        }

        var previousEndX: Float? = null
        textPositions.forEach { position ->
            val unicode = position.unicode.orEmpty()
            val x = position.xDirAdj
            val hasLargeGap = previousEndX?.let { x - it > 3.5f } == true

            if (unicode.isBlank() || hasLargeGap) {
                flush()
            }

            if (unicode.isNotBlank()) {
                current += position
                previousEndX = position.xDirAdj + position.widthDirAdj
            } else {
                previousEndX = null
            }
        }

        flush()
    }
}
