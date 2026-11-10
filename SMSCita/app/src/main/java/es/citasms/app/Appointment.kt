package es.citasms.app

data class Appointment(
    val id: String,
    val sender: String,
    val body: String,
    val title: String,
    val dateMillis: Long?,
    val timeHour: Int?,
    val timeMinute: Int?,
    val place: String?,
    val receivedAt: Long
)
