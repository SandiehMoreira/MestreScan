package br.com.mestrecell.mestrescan.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** O celular ligou: agenda a análise para depois da janela de observação. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action in BOOT_ACTIONS) BootAnalysisWorker.schedule(context)
    }

    private companion object {
        val BOOT_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON",
        )
    }
}
