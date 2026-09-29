package br.com.mestrecell.mestrescan

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import br.com.mestrecell.mestrescan.ui.MainViewModel
import br.com.mestrecell.mestrescan.ui.MestreScanApp
import br.com.mestrecell.mestrescan.ui.theme.MestreScanTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)
        setContent {
            MestreScanTheme {
                MestreScanApp(viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.getStringExtra(EXTRA_OPEN) == OPEN_BOOT) viewModel.openBootTimeline()
    }

    companion object {
        const val EXTRA_OPEN = "open"
        const val OPEN_BOOT = "boot"
    }
}
