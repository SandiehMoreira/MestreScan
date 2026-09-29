package br.com.mestrecell.mestrescan.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import br.com.mestrecell.mestrescan.ui.AppRow
import br.com.mestrecell.mestrescan.ui.MainViewModel
import br.com.mestrecell.mestrescan.ui.RiskBadge
import br.com.mestrecell.mestrescan.ui.Screen
import br.com.mestrecell.mestrescan.ui.ScreenFrame
import br.com.mestrecell.mestrescan.ui.SectionCard
import br.com.mestrecell.mestrescan.ui.UiState
import br.com.mestrecell.mestrescan.ui.formatDate

@Composable
fun HiddenAppsScreen(state: UiState, viewModel: MainViewModel) {
    val hidden = state.results.orEmpty()
        .filter { !it.app.hasLauncherIcon }
        .sortedByDescending { it.score }

    ScreenFrame(title = "Apps escondidos", onBack = { viewModel.back() }) {
        item {
            SectionCard {
                Text("Apps instalados sem ícone na tela", fontWeight = FontWeight.Bold)
                Text(
                    "Alguns são normais (teclados, serviços de outros apps). Mas vírus de propaganda " +
                        "costuma se esconder assim para você não achar e não desinstalar.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        when {
            state.results == null -> item { CircularProgressIndicator() }
            hidden.isEmpty() -> item {
                Text("Nenhum app escondido ✅", modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.titleMedium)
            }
            else -> items(hidden, key = { it.app.packageName }) { result ->
                SectionCard(modifier = Modifier.clickable { viewModel.open(Screen.Detail(result.app.packageName)) }) {
                    AppRow(
                        packageName = result.app.packageName,
                        title = result.app.label,
                        subtitle = "Instalado em ${formatDate(result.app.installedAt)}",
                    ) { RiskBadge(result.level) }
                }
            }
        }
    }
}
