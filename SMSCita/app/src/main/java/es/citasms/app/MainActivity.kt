package es.citasms.app

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.CalendarContract
import android.provider.Telephony
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.text.DateFormat
import java.util.Calendar

class MainActivity : Activity() {
    private lateinit var container: LinearLayout
    private val permissionRequest = 44

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
        requestSmsPermissionsIfNeeded()
    }

    private fun buildUi() {
        val scroll = ScrollView(this)
        container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }
        scroll.addView(container)
        setContentView(scroll)
        render()
    }

    private fun requestSmsPermissionsIfNeeded() {
        val needed = arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS)
            .filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (needed.isNotEmpty()) requestPermissions(needed.toTypedArray(), permissionRequest)
        else scanOldMessages()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, results: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, results)
        if (requestCode == permissionRequest) {
            if (checkSelfPermission(Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED) {
                scanOldMessages()
            } else {
                Toast.makeText(this, "Sin permiso de lectura no se pueden analizar los SMS antiguos.", Toast.LENGTH_LONG).show()
            }
            render()
        }
    }

    private fun scanOldMessages() {
        if (checkSelfPermission(Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) return
        runCatching {
            val projection = arrayOf("_id", "address", "body", "date")
            contentResolver.query(Telephony.Sms.CONTENT_URI, projection, null, null, "date DESC")?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow("_id")
                val addressCol = cursor.getColumnIndexOrThrow("address")
                val bodyCol = cursor.getColumnIndexOrThrow("body")
                val dateCol = cursor.getColumnIndexOrThrow("date")
                var count = 0
                while (cursor.moveToNext() && count < 3000) {
                    val id = cursor.getString(idCol)
                    val sender = cursor.getString(addressCol).orEmpty()
                    val body = cursor.getString(bodyCol).orEmpty()
                    val date = cursor.getLong(dateCol)
                    SmsAnalyzer.analyze("old_$id", sender, body, date)?.let { Store.add(this, it) }
                    count++
                }
            }
        }.onFailure {
            Toast.makeText(this, "No se pudo leer el historial SMS: ${it.message}", Toast.LENGTH_LONG).show()
        }
        render()
    }

    private fun render() {
        if (!::container.isInitialized) return
        container.removeAllViews()
        container.addView(TextView(this).apply {
            text = "CitaSMS"
            textSize = 28f
            setPadding(0, 0, 0, 8)
        })
        container.addView(TextView(this).apply {
            text = "Citas detectadas en tus SMS. Revisa los datos antes de guardarlos en Google Calendar."
            textSize = 15f
            setPadding(0, 0, 0, 18)
        })
        container.addView(Button(this).apply {
            text = "Volver a analizar SMS antiguos"
            setOnClickListener { scanOldMessages() }
        })
        val items = Store.all(this)
        if (items.isEmpty()) {
            container.addView(TextView(this).apply {
                text = "\nTodavía no se han encontrado posibles citas.\n\nComprueba que has concedido los permisos de SMS."
                textSize = 16f
            })
        }
        items.forEach { item ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(20, 18, 20, 18)
                setBackgroundResource(android.R.drawable.dialog_holo_light_frame)
            }
            card.addView(TextView(this).apply {
                text = item.title
                textSize = 19f
            })
            val dateText = item.dateMillis?.let { DateFormat.getDateInstance(DateFormat.MEDIUM).format(it) } ?: "Fecha no identificada"
            val timeText = if (item.timeHour != null) String.format("%02d:%02d", item.timeHour, item.timeMinute ?: 0) else "Hora no identificada"
            card.addView(TextView(this).apply {
                text = "$dateText · $timeText\nRemitente: ${item.sender}\n${item.place ?: "Lugar no identificado"}"
                textSize = 14f
                setPadding(0, 8, 0, 8)
            })
            card.addView(TextView(this).apply {
                text = item.body
                textSize = 13f
                setPadding(0, 4, 0, 10)
            })
            card.addView(Button(this).apply {
                text = if (Store.isImported(this@MainActivity, item.id)) "Volver a abrir Calendar" else "Revisar y añadir a Calendar"
                setOnClickListener { openCalendar(item) }
            })
            val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            params.setMargins(0, 12, 0, 12)
            container.addView(card, params)
        }
    }

    private fun openCalendar(item: Appointment) {
        val intent = Intent(Intent.ACTION_INSERT).setData(CalendarContract.Events.CONTENT_URI)
            .putExtra(CalendarContract.Events.TITLE, item.title)
            .putExtra(CalendarContract.Events.DESCRIPTION, "Detectado en SMS de ${item.sender}:\n${item.body}")
        item.dateMillis?.let { date ->
            val start = Calendar.getInstance().apply {
                timeInMillis = date
                if (item.timeHour != null) {
                    set(Calendar.HOUR_OF_DAY, item.timeHour)
                    set(Calendar.MINUTE, item.timeMinute ?: 0)
                }
            }
            val end = (start.clone() as Calendar).apply { add(Calendar.HOUR_OF_DAY, 1) }
            intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, start.timeInMillis)
            intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, end.timeInMillis)
        }
        item.place?.let { intent.putExtra(CalendarContract.Events.EVENT_LOCATION, it) }
        try {
            startActivity(intent)
            Store.markImported(this, item.id)
        } catch (_: Exception) {
            Toast.makeText(this, "No se encontró una aplicación de calendario compatible.", Toast.LENGTH_LONG).show()
        }
    }
}
