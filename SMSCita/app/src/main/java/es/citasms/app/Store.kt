package es.citasms.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object Store {
    private const val PREFS = "citasms_local"
    private const val KEY = "appointments"

    @Synchronized
    fun all(context: Context): List<Appointment> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "[]") ?: "[]"
        val array = JSONArray(raw)
        return (0 until array.length()).mapNotNull { i ->
            runCatching {
                val o = array.getJSONObject(i)
                Appointment(
                    id = o.getString("id"), sender = o.optString("sender"),
                    body = o.optString("body"), title = o.optString("title"),
                    dateMillis = if (o.isNull("date")) null else o.getLong("date"),
                    timeHour = if (o.isNull("hour")) null else o.getInt("hour"),
                    timeMinute = if (o.isNull("minute")) null else o.getInt("minute"),
                    place = o.optString("place").takeIf { it.isNotBlank() },
                    receivedAt = o.optLong("receivedAt")
                )
            }.getOrNull()
        }.sortedByDescending { it.receivedAt }
    }

    @Synchronized
    fun add(context: Context, item: Appointment) {
        val current = all(context)
        if (current.any { it.id == item.id }) return
        val next = (listOf(item) + current).take(500)
        val array = JSONArray()
        next.forEach { a ->
            array.put(JSONObject().apply {
                put("id", a.id); put("sender", a.sender); put("body", a.body)
                put("title", a.title)
                put("date", a.dateMillis ?: JSONObject.NULL)
                put("hour", a.timeHour ?: JSONObject.NULL)
                put("minute", a.timeMinute ?: JSONObject.NULL)
                put("place", a.place ?: "")
                put("receivedAt", a.receivedAt)
            })
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, array.toString()).apply()
    }

    fun markImported(context: Context, id: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean("imported_$id", true).apply()
    }

    fun isImported(context: Context, id: String) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("imported_$id", false)
}
