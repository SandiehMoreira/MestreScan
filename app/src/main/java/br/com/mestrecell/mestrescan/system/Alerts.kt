package br.com.mestrecell.mestrescan.system

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import br.com.mestrecell.mestrescan.MainActivity
import br.com.mestrecell.mestrescan.R
import br.com.mestrecell.mestrescan.boot.BootLaunch

object Alerts {
    const val CHANNEL_ALERTS = "alerts"
    /** Canal separado: quem achar demais silencia só as ofertas, não os alertas. */
    const val CHANNEL_OFFERS = "offers"
    private const val BOOT_NOTIFICATION_ID = 1001

    fun ensureChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ALERTS, "Alertas de segurança", NotificationManager.IMPORTANCE_HIGH)
                .apply { description = "Avisa quando um app abre propaganda sozinho" }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_OFFERS,
                "Ofertas da ${context.getString(R.string.brand_store)}",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = "Promoções e novidades da loja" }
        )
    }

    fun bootSuspects(context: Context, suspects: List<BootLaunch>) {
        val first = suspects.minBy { it.secondsAfterBoot }
        val title = if (suspects.size == 1) {
            "Achamos quem abre propaganda ao ligar"
        } else {
            "${suspects.size} apps abriram sozinhos ao ligar"
        }
        val text = "${first.label} abriu ${first.secondsAfterBoot} s depois de ligar" +
            (if (first.adScreen) " (tela de propaganda)." else ".") + " Toque para ver e remover."
        val open = Intent(context, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_OPEN, MainActivity.OPEN_BOOT)
        show(context, CHANNEL_ALERTS, BOOT_NOTIFICATION_ID, title, text, open, NotificationCompat.PRIORITY_HIGH)
    }

    /** Oferta recebida com o app aberto (com o app fechado, o próprio Firebase mostra). */
    fun offer(context: Context, title: String, text: String, extras: Map<String, String>) {
        val open = Intent(context, MainActivity::class.java)
        extras.forEach { (key, value) -> open.putExtra(key, value) }
        show(context, CHANNEL_OFFERS, (System.currentTimeMillis() % Int.MAX_VALUE).toInt(), title, text, open,
            NotificationCompat.PRIORITY_DEFAULT)
    }

    private fun show(
        context: Context,
        channel: String,
        id: Int,
        title: String,
        text: String,
        intent: Intent,
        priority: Int,
    ) {
        if (!Permissions.hasNotifications(context)) return
        ensureChannels(context)
        val pending = PendingIntent.getActivity(
            context,
            id,
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_stat_shield)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(priority)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (e: SecurityException) {
            // Permissão retirada entre a checagem e o envio.
        }
    }
}
