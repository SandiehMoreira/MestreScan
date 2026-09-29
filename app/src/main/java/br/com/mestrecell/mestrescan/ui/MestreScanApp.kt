package br.com.mestrecell.mestrescan.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.mestrecell.mestrescan.data.InstalledApp
import br.com.mestrecell.mestrescan.system.SystemActions
import br.com.mestrecell.mestrescan.ui.screens.BootTimelineScreen
import br.com.mestrecell.mestrescan.ui.screens.DetailScreen
import br.com.mestrecell.mestrescan.ui.screens.HelpScreen
import br.com.mestrecell.mestrescan.ui.screens.HiddenAppsScreen
import br.com.mestrecell.mestrescan.ui.screens.HomeScreen
import br.com.mestrecell.mestrescan.ui.screens.ResultsScreen
import kotlinx.coroutines.launch

/**
 * Fila de desinstalação: abre a tela do Android para um app de cada vez.
 * O usuário confirma (ou cancela) cada um.
 */
class RemovalQueue {
    var queue by mutableStateOf(emptyList<InstalledApp>())
    var total by mutableIntStateOf(0)
    var removed by mutableIntStateOf(0)
    var needsAdminStep by mutableStateOf(emptyList<InstalledApp>())
    var showSummary by mutableStateOf(false)
}

@Composable
fun MestreScanApp(viewModel: MainViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val removal = remember { RemovalQueue() }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onResume() }
    BackHandler(enabled = state.stack.size > 1) { viewModel.back() }

    val uninstallLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val app = removal.queue.firstOrNull() ?: return@rememberLauncherForActivityResult
        scope.launch {
            if (viewModel.confirmRemoved(app.packageName)) removal.removed++
            removal.queue = removal.queue.drop(1)
            if (removal.queue.isEmpty()) {
                if (removal.total > 1 || removal.needsAdminStep.isNotEmpty()) {
                    removal.showSummary = true
                } else if (removal.removed == 1) {
                    Toast.makeText(context, "${app.label} removido ✅", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    LaunchedEffect(removal.queue) {
        removal.queue.firstOrNull()?.let { uninstallLauncher.launch(SystemActions.uninstall(it.packageName)) }
    }

    fun remove(apps: List<InstalledApp>) {
        // Administrador ativo bloqueia a desinstalação: esses precisam de um passo antes.
        val (admins, normal) = apps.partition { it.isDeviceAdmin }
        removal.removed = 0
        removal.total = apps.size
        removal.needsAdminStep = admins
        removal.queue = normal
        if (normal.isEmpty() && admins.isNotEmpty()) removal.showSummary = true
    }

    when (val screen = state.screen) {
        Screen.Home -> HomeScreen(state, viewModel)
        Screen.Results -> ResultsScreen(state, viewModel, onRemove = ::remove)
        is Screen.Detail -> DetailScreen(state, viewModel, screen.packageName, onRemove = { remove(listOf(it)) })
        Screen.Hidden -> HiddenAppsScreen(state, viewModel)
        Screen.BootTimeline -> BootTimelineScreen(state, viewModel)
        Screen.Help -> HelpScreen(viewModel)
    }

    if (removal.showSummary) {
        RemovalSummaryDialog(removal, onDismiss = { removal.showSummary = false })
    }
}

@Composable
private fun RemovalSummaryDialog(removal: RemovalQueue, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val attempted = removal.total - removal.needsAdminStep.size
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (removal.removed == removal.total) "Celular limpo pelo MestreScan ✅"
                else "Limpeza concluída"
            )
        },
        text = {
            val lines = buildList {
                if (attempted > 0) add("${removal.removed} de $attempted app(s) removido(s).")
                if (removal.needsAdminStep.isNotEmpty()) {
                    add(
                        "Precisam de um passo antes (são administradores do celular): " +
                            removal.needsAdminStep.joinToString { it.label } +
                            ". Abra cada um e toque em \"Tirar administrador\"."
                    )
                }
            }
            Text(lines.joinToString("\n\n"))
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } },
        dismissButton = if (removal.needsAdminStep.isNotEmpty()) {
            {
                TextButton(onClick = {
                    SystemActions.openDeviceAdminSettings(context)
                    onDismiss()
                }) { Text("Abrir administradores") }
            }
        } else null,
    )
}
