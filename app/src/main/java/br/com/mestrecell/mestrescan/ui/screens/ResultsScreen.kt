package br.com.mestrecell.mestrescan.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.mestrecell.mestrescan.R
import br.com.mestrecell.mestrescan.data.InstalledApp
import br.com.mestrecell.mestrescan.data.RiskLevel
import br.com.mestrecell.mestrescan.data.ScanResult
import br.com.mestrecell.mestrescan.ui.AppRow
import br.com.mestrecell.mestrescan.ui.MainViewModel
import br.com.mestrecell.mestrescan.ui.RiskBadge
import br.com.mestrecell.mestrescan.ui.Screen
import br.com.mestrecell.mestrescan.ui.ScreenFrame
import br.com.mestrecell.mestrescan.ui.SectionCard
import br.com.mestrecell.mestrescan.ui.StoreFooter
import br.com.mestrecell.mestrescan.ui.UiState
import br.com.mestrecell.mestrescan.ui.WhatsAppCta
import br.com.mestrecell.mestrescan.ui.plural
import br.com.mestrecell.mestrescan.ui.theme.RiskColors

@Composable
fun ResultsScreen(state: UiState, viewModel: MainViewModel, onRemove: (List<InstalledApp>) -> Unit) {
    val suspects = state.suspects
    val dangers = suspects.filter { it.level == RiskLevel.DANGER }
    val trustedResults = state.results.orEmpty().filter { it.app.packageName in state.trusted }
    var showTrusted by rememberSaveable { mutableStateOf(false) }

    ScreenFrame(title = "Resultado", onBack = { viewModel.back() }) {
        item {
            if (suspects.isEmpty()) {
                SectionCard(borderColor = RiskColors.safe) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = RiskColors.safe, modifier = Modifier.size(40.dp))
                    Text("Nenhum app suspeito", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    val analyzed = state.results.orEmpty().size
                    Text(
                        "Seu celular está protegido pela ${stringResource(R.string.brand_store)}. " +
                            if (analyzed == 0) "Não há apps baixados, só os do sistema."
                            else "${plural(analyzed, "app analisado", "apps analisados")}.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                SectionCard(borderColor = if (dangers.isNotEmpty()) RiskColors.danger else RiskColors.suspect) {
                    Text(
                        "Encontramos ${plural(suspects.size, "app", "apps")} para revisar",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        plural(dangers.size, "perigoso", "perigosos") + " · " +
                            plural(suspects.size - dangers.size, "suspeito", "suspeitos") + " · " +
                            plural(state.results.orEmpty().size, "analisado", "analisados"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "Você decide: toque em um app para ver o motivo. Nada é apagado sem você confirmar.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    if (dangers.isNotEmpty()) {
                        Button(
                            onClick = { onRemove(dangers.map { it.app }) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = RiskColors.danger),
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Limpar todos os perigosos (${dangers.size})", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        items(suspects, key = { it.app.packageName }) { result ->
            SuspectCard(
                result = result,
                onOpen = { viewModel.open(Screen.Detail(result.app.packageName)) },
                onRemove = { onRemove(listOf(result.app)) },
            )
        }
        if (trustedResults.isNotEmpty()) {
            item {
                TextButton(onClick = { showTrusted = !showTrusted }) {
                    Text(if (showTrusted) "Esconder apps confiáveis" else "Apps que você marcou como confiáveis (${trustedResults.size})")
                }
            }
            if (showTrusted) {
                items(trustedResults, key = { "trusted-" + it.app.packageName }) { result ->
                    SectionCard {
                        AppRow(result.app.packageName, result.app.label, "Marcado como confiável") {
                            TextButton(onClick = { viewModel.untrust(result.app.packageName) }) { Text("Analisar de novo") }
                        }
                    }
                }
            }
        }
        item { WhatsAppCta() }
        item { StoreFooter() }
    }
}

@Composable
private fun SuspectCard(result: ScanResult, onOpen: () -> Unit, onRemove: () -> Unit) {
    SectionCard(
        modifier = Modifier.clickable(onClick = onOpen),
        borderColor = RiskColors.of(result.level).copy(alpha = 0.6f),
    ) {
        AppRow(
            packageName = result.app.packageName,
            title = result.app.label,
            subtitle = result.reasons.take(2).joinToString(" · ") { it.text },
        ) {
            RiskBadge(result.level)
        }
        OutlinedButton(onClick = onRemove, modifier = Modifier.align(Alignment.End)) {
            Text("Remover")
        }
    }
}
