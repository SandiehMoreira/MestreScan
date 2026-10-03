package br.com.mestrecell.mestrescan.system

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import br.com.mestrecell.mestrescan.BuildConfig
import br.com.mestrecell.mestrescan.R

/**
 * Atalhos para as telas do Android. O MestreScan nunca remove nada sozinho:
 * só abre a tela certa e o usuário confirma.
 */
object SystemActions {

    fun uninstall(packageName: String): Intent =
        Intent(Intent.ACTION_DELETE, Uri.parse("package:$packageName"))

    fun openAppDetails(context: Context, packageName: String) =
        start(context, Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))

    fun openOverlaySettings(context: Context, packageName: String) =
        start(
            context,
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")),
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION),
        )

    fun openAccessibilitySettings(context: Context) =
        start(context, Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))

    fun openUsageAccessSettings(context: Context) =
        start(
            context,
            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, Uri.parse("package:${context.packageName}")),
            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS),
        )

    fun openLink(context: Context, uri: Uri) = start(context, Intent(Intent.ACTION_VIEW, uri))

    /** Tela do Android para silenciar só as ofertas, mantendo os alertas. */
    fun openOffersChannelSettings(context: Context) =
        start(
            context,
            Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                .putExtra(Settings.EXTRA_CHANNEL_ID, Alerts.CHANNEL_OFFERS),
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
        )

    /** A lista de administradores não tem atalho oficial; tentamos os caminhos conhecidos. */
    fun openDeviceAdminSettings(context: Context) =
        start(
            context,
            Intent().setComponent(ComponentName("com.android.settings", "com.android.settings.Settings\$DeviceAdminSettingsActivity")),
            Intent().setComponent(ComponentName("com.android.settings", "com.android.settings.DeviceAdminSettings")),
            Intent(Settings.ACTION_SECURITY_SETTINGS),
        )

    fun openWhatsApp(context: Context, message: String = context.getString(R.string.brand_whatsapp_message)) {
        val url = "https://wa.me/${BuildConfig.WHATSAPP_NUMBER}?text=${Uri.encode(message)}"
        start(context, Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    /** Tenta cada intent em ordem até uma abrir. */
    private fun start(context: Context, vararg intents: Intent) {
        for (intent in intents) {
            try {
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return
            } catch (e: ActivityNotFoundException) {
                continue
            } catch (e: SecurityException) {
                continue
            }
        }
        Toast.makeText(context, "Não consegui abrir essa tela neste celular.", Toast.LENGTH_LONG).show()
    }
}
