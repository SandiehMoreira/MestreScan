package br.com.mestrecell.mestrescan

import android.app.Application
import br.com.mestrecell.mestrescan.system.Alerts
import br.com.mestrecell.mestrescan.system.Offers

class MestreScanApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Os canais precisam existir antes de chegar uma oferta com o app fechado.
        Alerts.ensureChannels(this)
        Offers.subscribe(this)
    }
}
