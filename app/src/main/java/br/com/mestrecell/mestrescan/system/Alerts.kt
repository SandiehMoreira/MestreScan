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
    private const val CHANNEL_ID = "alerts"
    private const val BOOT_NOTIFICATION_ID = 1001

    fun bootSuspects(context: Context, suspects: List<BootLaunch>) {
        if (!Permissions.hasNotifications(context)) return
        ensureChannel(context)

        val first = suspects.minBy { it.secondsAfterBoot }
        val title = if (suspects.size == 1) {
            "Achamos quem abre propaganda ao ligar"
        } else {
            "${suspects.size} apps abriram sozinhos ao ligar"
        }
        val text = "${first.label} abriu ${first.secondsAfterBoot} s depois de ligar" +
            (if (first.adScreen) " (tela de propaganda)." else ".") + " Toque para ver e remover."

        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_OPEN, MainActivity.OPEN_BOOT)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_shield)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(BOOT_NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // Permissão retirada entre a checagem e o envio.
        }
    }

    private fun ensureChannel(context: Context) {
        val channel = NotificationChannel(CHANNEL_ID, "Alertas de segurança", NotificationManager.IMPORTANCE_HIGH)
            .apply { description = "Avisa quando um app abre propaganda sozinho" }
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }
}
