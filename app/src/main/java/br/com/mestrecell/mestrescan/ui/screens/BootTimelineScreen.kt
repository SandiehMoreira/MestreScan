package br.com.mestrecell.mestrescan.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.mestrecell.mestrescan.boot.BootEvidence
import br.com.mestrecell.mestrescan.boot.BootLaunch
import br.com.mestrecell.mestrescan.boot.BootObserver
import br.com.mestrecell.mestrescan.boot.BootRecord
import br.com.mestrecell.mestrescan.system.SystemActions
import br.com.mestrecell.mestrescan.ui.AppRow
import br.com.mestrecell.mestrescan.ui.MainViewModel
import br.com.mestrecell.mestrescan.ui.Screen
import br.com.mestrecell.mestrescan.ui.ScreenFrame
import br.com.mestrecell.mestrescan.ui.SectionCard
import br.com.mestrecell.mestrescan.ui.UiState
import br.com.mestrecell.mestrescan.ui.WhatsAppCta
import br.com.mestrecell.mestrescan.ui.formatDateTime
import br.com.mestrecell.mestrescan.ui.plural
import br.com.mestrecell.mestrescan.ui.theme.RiskColors

@Composable
fun BootTimelineScreen(state: UiState, viewModel: MainViewModel) {
    val context = LocalContext.current
    val trusted = state.trusted
    val evidence = BootObserver.evidence(state.boots).filterKeys { it !in trusted }
    val culprits = evidence.entries.sortedWith(
        compareByDescending<Map.Entry<String, BootEvidence>> { it.value.bootsWithLaunch }
            .thenBy { it.value.firstSeconds }
    )
    val labels = state.boots.flatMap { it.launches }.associate { it.packageName to it.label }
    val secondsSinceBoot = ((System.currentTimeMillis() - viewModel.currentBootTime()) / 1000).toInt()
    val stillObserving = state.hasUsageAccess && secondsSinceBoot < viewModel.bootWindowSeconds

    ScreenFrame(title = "O que abriu ao ligar", onBack = { viewModel.back() }) {
        item {
            SectionCard {
                Text("Como funciona", fontWeight = FontWeight.Bold)
                Text(
                    "Vírus de propaganda costuma começar uns 30 segundos depois de ligar o celular. " +
                        "O MestreScan registra quais apps abrem sozinhos nos primeiros minutos e aponta o culpado.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (!state.hasUsageAccess) {
            item {
                SectionCard(borderColor = MaterialTheme.colorScheme.primary) {
                    Text("Precisa liberar o acesso ao uso", fontWeight = FontWeight.Bold)
                    Text(
                        "Sem essa permissão o Android não mostra quem abriu tela depois de ligar.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedButton(onClick = { SystemActions.openUsageAccessSettings(context) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Liberar agora")
                    }
                }
            }
            return@ScreenFrame
        }

        item {
            SectionCard(borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)) {
                Text("Diagnóstico de reinício", fontWeight = FontWeight.Bold)
                Text("1. Reinicie o celular.")
                Text("2. Espere a propaganda aparecer (ou uns 3 minutos).")
                Text("3. Abra o MestreScan: o culpado aparece aqui e chega uma notificação.")
                if (stillObserving) {
                    Text(
                        "Ainda observando este reinício (até ${viewModel.bootWindowSeconds / 60} min depois de ligar)…",
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                OutlinedButton(onClick = { viewModel.refreshBoot() }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Refresh, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Atualizar agora")
                }
            }
        }

        if (culprits.isNotEmpty()) {
            item {
                Text("Apontados pelo MestreScan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            items(culprits, key = { "culprit-" + it.key }) { (pkg, ev) ->
                val installed = state.resultFor(pkg) != null
                SectionCard(
                    modifier = if (installed) Modifier.clickable { viewModel.open(Screen.Detail(pkg)) } else Modifier,
                    borderColor = RiskColors.danger.copy(alpha = 0.6f),
                ) {
                    AppRow(
                        packageName = pkg,
                        title = labels[pkg] ?: pkg,
                        subtitle = "Abriu sozinho em ${ev.bootsWithLaunch} de ${plural(ev.totalBoots, "vez", "vezes")} que o celular ligou" +
                            " · a partir de ${ev.firstSeconds} s" +
                            (if (ev.adScreen) " · tela de propaganda" else ""),
                    ) {
                        if (installed) Text("Ver", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item { Text("Histórico de reinícios", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        if (state.boots.isEmpty()) {
            item {
                Text(
                    "Nenhum reinício registrado ainda. Reinicie o celular e volte aqui.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(state.boots, key = { it.bootTime }) { record -> BootCard(record, trusted) }
        item { WhatsAppCta() }
    }
}

@Composable
private fun BootCard(record: BootRecord, trusted: Set<String>) {
    val suspicious = record.launches.any { it.selfStarted && it.packageName !in trusted }
    SectionCard(borderColor = if (suspicious) RiskColors.danger.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline) {
        Text("Ligou ${formatDateTime(record.bootTime)}", fontWeight = FontWeight.Bold)
        if (record.launches.isEmpty()) {
            Text("Nenhum app baixado abriu nos primeiros minutos ✅", color = RiskColors.safe)
        }
        record.launches.sortedBy { it.secondsAfterBoot }.forEach { LaunchLine(it, trusted) }
    }
}

@Composable
private fun LaunchLine(launch: BootLaunch, trusted: Set<String>) {
    val flagged = launch.selfStarted && launch.packageName !in trusted
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            "+${launch.secondsAfterBoot} s",
            modifier = Modifier.width(64.dp),
            color = if (flagged) RiskColors.danger else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
        )
        val details = buildList {
            add(if (launch.selfStarted) "abriu sozinho" else "provavelmente você abriu")
            if (launch.adScreen) add("tela de propaganda")
            if (launch.beforeUnlock) add("antes de desbloquear")
            if (!launch.hasIcon) add("app escondido")
        }
        Text(
            "${launch.label} — ${details.joinToString(", ")}",
            color = if (flagged) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
