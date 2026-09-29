package br.com.mestrecell.mestrescan.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.mestrecell.mestrescan.data.InstalledApp
import br.com.mestrecell.mestrescan.data.OverlayState
import br.com.mestrecell.mestrescan.system.SystemActions
import br.com.mestrecell.mestrescan.ui.AppIcon
import br.com.mestrecell.mestrescan.ui.MainViewModel
import br.com.mestrecell.mestrescan.ui.RiskBadge
import br.com.mestrecell.mestrescan.ui.ScreenFrame
import br.com.mestrecell.mestrescan.ui.SectionCard
import br.com.mestrecell.mestrescan.ui.UiState
import br.com.mestrecell.mestrescan.ui.WhatsAppCta
import br.com.mestrecell.mestrescan.ui.formatDate
import br.com.mestrecell.mestrescan.ui.formatDateTime
import br.com.mestrecell.mestrescan.ui.theme.RiskColors

@Composable
fun DetailScreen(state: UiState, viewModel: MainViewModel, packageName: String, onRemove: (InstalledApp) -> Unit) {
    val context = LocalContext.current
    val result = state.resultFor(packageName)
    var askAdminFirst by remember { mutableStateOf(false) }

    ScreenFrame(title = "Detalhe do app", onBack = { viewModel.back() }) {
        if (result == null) {
            item {
                SectionCard(borderColor = RiskColors.safe) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = RiskColors.safe, modifier = Modifier.size(40.dp))
                    Text("Este app não está mais no celular.", fontWeight = FontWeight.Bold)
                }
            }
            return@ScreenFrame
        }
        val app = result.app
        val isTrusted = packageName in state.trusted

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(packageName, size = 64.dp)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(app.label, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(packageName, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    RiskBadge(result.level, result.score)
                }
            }
        }

        if (result.reasons.isNotEmpty()) {
            item {
                SectionCard(borderColor = RiskColors.of(result.level).copy(alpha = 0.6f)) {
                    Text("Por que foi apontado", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    result.reasons.forEach { reason ->
                        Row {
                            Text("• ${reason.text}", modifier = Modifier.weight(1f))
                            Text("+${reason.points}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        item {
            SectionCard {
                Text("Informações", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                InfoLine("Instalado em", formatDate(app.installedAt))
                InfoLine(
                    "Origem",
                    when (app.installer) {
                        "com.android.vending" -> "Play Store"
                        null -> "Desconhecida (APK de fora)"
                        else -> "Fora da Play Store (${app.installer})"
                    },
                )
                InfoLine("Ícone na tela", if (app.hasLauncherIcon) "Sim" else "Não (escondido)")
                InfoLine("Administrador", if (app.isDeviceAdmin) "Sim" else "Não")
            }
        }

        val bootLaunches = state.boots.flatMap { boot ->
            boot.launches.filter { it.packageName == packageName && it.selfStarted }.map { boot.bootTime to it }
        }
        if (bootLaunches.isNotEmpty()) {
            item {
                SectionCard(borderColor = RiskColors.danger.copy(alpha = 0.6f)) {
                    Text("Quando o celular ligou", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    bootLaunches.forEach { (bootTime, launch) ->
                        Text(
                            "Ligou ${formatDateTime(bootTime)} → abriu sozinho +${launch.secondsAfterBoot} s" +
                                (if (launch.adScreen) " (tela de propaganda)" else ""),
                        )
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (app.isDeviceAdmin) {
                    Text(
                        "Este app é administrador do celular. Primeiro tire o administrador, depois remova.",
                        color = RiskColors.suspect,
                    )
                    OutlinedButton(onClick = { SystemActions.openDeviceAdminSettings(context) }, modifier = Modifier.fillMaxWidth()) {
                        Text("1. Tirar administrador")
                    }
                }
                Button(
                    onClick = { if (app.isDeviceAdmin) askAdminFirst = true else onRemove(app) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = RiskColors.danger),
                ) {
                    Text(if (app.isDeviceAdmin) "2. Remover" else "Remover", fontWeight = FontWeight.Bold)
                }
                if (app.overlay != OverlayState.NONE) {
                    OutlinedButton(onClick = { SystemActions.openOverlaySettings(context, packageName) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Tirar permissão de aparecer por cima")
                    }
                }
                if (app.hasAccessibility) {
                    OutlinedButton(onClick = { SystemActions.openAccessibilitySettings(context) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Tirar acessibilidade")
                    }
                }
                OutlinedButton(onClick = { SystemActions.openAppDetails(context, packageName) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Abrir nas configurações")
                }
                TextButton(
                    onClick = {
                        if (isTrusted) viewModel.untrust(packageName) else {
                            viewModel.trust(packageName)
                            viewModel.back()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (isTrusted) "Voltar a analisar este app" else "Conheço e confio neste app (não mostrar mais)")
                }
            }
        }
        item { WhatsAppCta() }
    }

    if (askAdminFirst) {
        AlertDialog(
            onDismissRequest = { askAdminFirst = false },
            title = { Text("Falta um passo") },
            text = {
                Text(
                    "Este app é administrador do celular, por isso o Android não deixa remover direto. " +
                        "Na próxima tela, toque nele e desative. Depois volte aqui e toque em Remover de novo."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    askAdminFirst = false
                    SystemActions.openDeviceAdminSettings(context)
                }) { Text("Tirar administrador") }
            },
            dismissButton = {
                TextButton(onClick = { askAdminFirst = false }) { Text("Cancelar") }
            },
        )
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, fontWeight = FontWeight.Medium)
    }
}
