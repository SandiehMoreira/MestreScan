package br.com.mestrecell.mestrescan.system

import android.content.Context
import android.content.Intent
import android.net.Uri
import br.com.mestrecell.mestrescan.R
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Ofertas da loja por notificação (Firebase Cloud Messaging).
 * Cada marca tem o seu tópico (R.string.brand_offers_topic); a loja envia
 * pelo console do Firebase. Ver docs/ENVIAR-OFERTAS.md.
 */
object Offers {
    /** Dados extras que a loja pode mandar junto com a oferta. */
    const val EXTRA_WHATSAPP = "whatsapp"
    const val EXTRA_LINK = "link"

    fun subscribe(context: Context) {
        // Sem google-services.json o Firebase não inicia; o resto do app segue normal.
        if (FirebaseApp.getApps(context).isEmpty()) return
        FirebaseMessaging.getInstance().subscribeToTopic(context.getString(R.string.brand_offers_topic))
    }

    /** Tocou numa oferta: abre o WhatsApp com a mensagem ou o link mandado pela loja. */
    fun handleTap(context: Context, intent: Intent?): Boolean {
        val whatsapp = intent?.getStringExtra(EXTRA_WHATSAPP)
        val link = intent?.getStringExtra(EXTRA_LINK)
        when {
            !whatsapp.isNullOrBlank() -> SystemActions.openWhatsApp(context, whatsapp)
            !link.isNullOrBlank() -> SystemActions.openLink(context, Uri.parse(link))
            else -> return false
        }
        intent?.removeExtra(EXTRA_WHATSAPP)
        intent?.removeExtra(EXTRA_LINK)
        return true
    }
}

class OffersMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        val notification = message.notification ?: return
        Alerts.offer(
            context = this,
            title = notification.title ?: getString(R.string.brand_store),
            text = notification.body.orEmpty(),
            extras = message.data,
        )
    }

    override fun onNewToken(token: String) {
        Offers.subscribe(this)
    }
}
