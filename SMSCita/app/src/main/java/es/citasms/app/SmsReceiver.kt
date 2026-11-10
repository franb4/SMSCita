package es.citasms.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isEmpty()) return
        val body = messages.joinToString("") { it.messageBody.orEmpty() }
        val sender = messages.firstOrNull()?.originatingAddress.orEmpty()
        val received = messages.firstOrNull()?.timestampMillis ?: System.currentTimeMillis()
        val id = "new_${sender}_${received}"
        SmsAnalyzer.analyze(id, sender, body, received)?.let { Store.add(context, it) }
    }
}
