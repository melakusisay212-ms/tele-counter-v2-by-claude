package et.teleexpense.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Processes each new SMS locally, only after tracking has started. Nothing is logged or sent anywhere. */
class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val parts = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (parts.isNullOrEmpty()) return
        val sender = parts[0].originatingAddress
        val body = parts.joinToString("") { it.messageBody.orEmpty() }
        val timestamp = parts[0].timestampMillis
        val repo = Repo.get(context)
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                repo.onIncoming(sender, body, timestamp)
                // The inbox row can land a moment after the broadcast; a short wait then a window scan
                // guarantees nothing is missed. Duplicates are ignored by hash.
                kotlinx.coroutines.delay(2500)
                repo.scanNew()
            } finally {
                pending.finish()
            }
        }
    }
}
