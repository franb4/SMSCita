package es.citasms.app

import java.text.Normalizer
import java.util.Calendar
import java.util.Locale

object SmsAnalyzer {
    private val appointmentWords = listOf(
        "cita", "予約", "consulta", "revision", "revisión", "visita",
        "reserv", "recordatorio", "confirmamos", "confirmada",
        "itv", "taller", "medico", "médico", "hospital", "centro de salud",
        "hora para", "acuda", "presentese", "preséntese"
    )

    private val dateNumeric = Regex("""\b([0-3]?\d)[/-]([01]?\d)[/-](20\d{2}|\d{2})\b""")
    private val timeRegex = Regex("""\b(?:a\s+las?\s+)?([01]?\d|2[0-3])[:.]([0-5]\d)\b""", RegexOption.IGNORE_CASE)
    private val hourOnly = Regex("""\ba\s+las?\s+([01]?\d|2[0-3])(?:\s*(?:h|horas))?\b""", RegexOption.IGNORE_CASE)

    private val months = mapOf(
        "enero" to 0, "febrero" to 1, "marzo" to 2, "abril" to 3,
        "mayo" to 4, "junio" to 5, "julio" to 6, "agosto" to 7,
        "septiembre" to 8, "setiembre" to 8, "octubre" to 9,
        "noviembre" to 10, "diciembre" to 11
    )

    fun analyze(id: String, sender: String, body: String, receivedAt: Long): Appointment? {
        val normalized = normalize(body)
        if (appointmentWords.none { normalize(it) in normalized }) return null

        val date = parseDate(body, receivedAt)
        val time = timeRegex.find(body)?.let {
            it.groupValues[1].toInt() to it.groupValues[2].toInt()
        } ?: hourOnly.find(body)?.let {
            it.groupValues[1].toInt() to 0
        }

        val title = when {
            "itv" in normalized -> "Cita ITV"
            "medico" in normalized || "consulta" in normalized || "hospital" in normalized -> "Cita médica"
            "taller" in normalized -> "Cita en taller"
            "dent" in normalized -> "Cita dental"
            "pelu" in normalized -> "Cita"
            else -> "Cita detectada por SMS"
        }

        val place = Regex("""(?i)\b(?:en|direccion|dirección|centro)\s*:?\s*([^.,;\n]{3,70})""")
            .find(body)?.groupValues?.getOrNull(1)?.trim()

        return Appointment(
            id = id,
            sender = sender,
            body = body,
            title = title,
            dateMillis = date,
            timeHour = time?.first,
            timeMinute = time?.second,
            place = place,
            receivedAt = receivedAt
        )
    }

    private fun parseDate(body: String, receivedAt: Long): Long? {
        val numeric = dateNumeric.find(body)
        if (numeric != null) {
            val day = numeric.groupValues[1].toInt()
            val month = numeric.groupValues[2].toInt() - 1
            var year = numeric.groupValues[3].toInt()
            if (year < 100) year += 2000
            return calendarDate(year, month, day)
        }

        val words = normalize(body)
        for ((monthName, monthIndex) in months) {
            val match = Regex("""\b(\d{1,2})\s+de\s+$monthName(?:\s+de\s+(20\d{2}))?\b""").find(words)
            if (match != null) {
                val day = match.groupValues[1].toInt()
                val year = match.groupValues[2].takeIf { it.isNotBlank() }?.toInt()
                    ?: Calendar.getInstance().apply { timeInMillis = receivedAt }.get(Calendar.YEAR)
                return calendarDate(year, monthIndex, day)
            }
        }
        return null
    }

    private fun calendarDate(year: Int, month: Int, day: Int): Long? = try {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    } catch (_: Exception) { null }

    private fun normalize(value: String): String =
        Normalizer.normalize(value.lowercase(Locale("es", "ES")), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
}
