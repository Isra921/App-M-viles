package com.escom.escomorienta.ui.schedule

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.escom.escomorienta.data.settings.SchedulePaletteMode
import com.escom.escomorienta.data.settings.subjectSelectionKey

@Immutable
data class SchedulePaletteColors(
    val accents: List<Color>,
    val onAccent: Color
)

@Composable
fun schedulePaletteColors(mode: SchedulePaletteMode): SchedulePaletteColors {
    return when (mode) {
        SchedulePaletteMode.INSTITUTIONAL -> SchedulePaletteColors(
            accents = listOf(
                Color(0xFF6C1D45), // Guinda clásico IPN
                Color(0xFF006699), // Azul institucional ESCOM
                Color(0xFFB38E5D), // Oro/Dorado Politécnico
                Color(0xFF00385C), // Azul marino profundo
                Color(0xFF821342), // Guinda vibrante IPN
                Color(0xFF9E783B), // Ocre politécnico
                Color(0xFF025884), // Azul cobalto ESCOM
                Color(0xFF5A1438), // Vino oscuro IPN
                Color(0xFF0B7285), // Turquesa institucional
                Color(0xFF8C6438)  // Bronce politécnico
            ),
            onAccent = Color.White
        )
        SchedulePaletteMode.PASTEL -> SchedulePaletteColors(
            accents = listOf(
                Color(0xFFC8B6FF), // Lavanda pastel
                Color(0xFFA8E6CF), // Menta fresca
                Color(0xFFFFD3B6), // Melocotón suave
                Color(0xFFBEE1E6), // Celeste cielo
                Color(0xFFFFC6FF), // Rosa pastel
                Color(0xFFFDE4CF), // Arena suave
                Color(0xFFC1DFD1), // Salvia suave
                Color(0xFFE2C4FF), // Liliácea
                Color(0xFFFFF1C5), // Crema pastel
                Color(0xFFB4E3E5)  // Turquesa glaciar suave
            ),
            onAccent = Color(0xFF1E293B) // Slate 800 - contraste óptimo sobre fondos claros
        )
        SchedulePaletteMode.WARM -> SchedulePaletteColors(
            accents = listOf(
                Color(0xFFD97706), // Ámbar intenso
                Color(0xFFC2410C), // Terracota cálido
                Color(0xFFEA580C), // Coral vibrante
                Color(0xFFB45309), // Ocre tostado
                Color(0xFF9A3412), // Teja quemada
                Color(0xFFCA8A04), // Mostaza dorada
                Color(0xFFBE123C), // Carmesí cálido
                Color(0xFFA16207), // Siena
                Color(0xFF854D0E), // Canela profunda
                Color(0xFF9F1239)  // Rubí oscuro
            ),
            onAccent = Color.White
        )
        SchedulePaletteMode.ACCESSIBLE -> SchedulePaletteColors(
            accents = listOf(
                Color(0xFF0056B3), // Azul intenso accesible
                Color(0xFFD35400), // Naranja quemado
                Color(0xFF008080), // Teal esmeralda
                Color(0xFF5B2C6F), // Púrpura profundo
                Color(0xFF196F3D), // Verde bosque
                Color(0xFFC0392B), // Bermellón
                Color(0xFF1B4F72), // Azul marino puro
                Color(0xFF7D1C4A), // Ciruela alto contraste
                Color(0xFF117A65), // Petróleo oscuro
                Color(0xFF6E2C00)  // Caramelo profundo
            ),
            onAccent = Color.White
        )
        SchedulePaletteMode.COLD -> SchedulePaletteColors(
            accents = listOf(
                Color(0xFF1D4ED8), // Azul zafiro
                Color(0xFF3730A3), // Índigo profundo
                Color(0xFF0E7490), // Océano ártico
                Color(0xFF0369A1), // Azul cobalto
                Color(0xFF3B82F6), // Azul cielo acero
                Color(0xFF0F766E), // Teal glaciar
                Color(0xFF4338CA), // Violeta frío
                Color(0xFF0284C7), // Cian polar
                Color(0xFF334155), // Pizarra fría
                Color(0xFF2563EB)  // Azul eléctrico frío
            ),
            onAccent = Color.White
        )
        SchedulePaletteMode.CUSTOM -> SchedulePaletteColors(
            accents = CustomScheduleColors,
            onAccent = Color.White
        )
    }
}

fun scheduleAccentFor(subject: String, palette: List<Color>): Color {
    val index = subject.lowercase().hashCode().mod(palette.size)
    return palette[index]
}

val CustomScheduleColors = listOf(
    Color(0xFF006699), Color(0xFF6C1D45), Color(0xFFD97706), Color(0xFF16A34A),
    Color(0xFF7C3AED), Color(0xFFEA580C), Color(0xFF0891B2), Color(0xFFDB2777),
    Color(0xFF4F46E5), Color(0xFF059669), Color(0xFFDC2626), Color(0xFF475569)
)

fun scheduleSubjectKey(subject: String): String = subjectSelectionKey(subject)

fun decodeCustomScheduleColors(value: String): Map<String, Int> = value
    .split(';')
    .mapNotNull { entry ->
        val parts = entry.split('=', limit = 2)
        val index = parts.getOrNull(1)?.toIntOrNull() ?: return@mapNotNull null
        parts.firstOrNull()?.takeIf { it.isNotBlank() }?.let { it to index }
    }
    .toMap()

fun encodeCustomScheduleColors(colors: Map<String, Int>): String = colors.entries
    .sortedBy { it.key }
    .joinToString(";") { "${it.key}=${it.value}" }

fun subjectAccentColors(
    subjects: List<String>,
    palette: List<Color>,
    customIndexes: Map<String, Int> = emptyMap()
): Map<String, Color> = subjects
    .distinctBy(::scheduleSubjectKey)
    .sortedBy(::scheduleSubjectKey)
    .mapIndexed { index, subject ->
        val key = scheduleSubjectKey(subject)
        val colorIndex = customIndexes[key] ?: index
        key to palette[colorIndex.mod(palette.size)]
    }
    .toMap()
