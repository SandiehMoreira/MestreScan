package br.com.mestrecell.mestrescan.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.mestrecell.mestrescan.R
import br.com.mestrecell.mestrescan.data.AppPrefs
import br.com.mestrecell.mestrescan.system.Permissions
import br.com.mestrecell.mestrescan.system.SystemActions
import br.com.mestrecell.mestrescan.ui.MainViewModel
import br.com.mestrecell.mestrescan.ui.Screen
import br.com.mestrecell.mestrescan.ui.ScreenFrame
import br.com.mestrecell.mestrescan.ui.SectionCard
import br.com.mestrecell.mestrescan.ui.StoreFooter
import br.com.mestrecell.mestrescan.ui.UiState
import br.com.mestrecell.mestrescan.ui.WhatsAppCta
import br.com.mestrecell.mestrescan.ui.formatDateTime
import br.com.mestrecell.mestrescan.ui.plural
import br.com.mestrecell.mestrescan.ui.theme.RiskColors

@Composable
fun HomeScreen(state: UiState, viewModel: MainViewModel) {
    val context = LocalContext.current
    val prefs = remember { AppPrefs(context) }
    var showNotificationsInfo by remember { mutableStateOf(false) }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    fun startScan() {
        // Na primeira vez, avisa para que servem as notificações antes de pedir.
        if (!prefs.notificationsInfoShown) {
            showNotificationsInfo = true
            return
        }
        viewModel.scan()
    }

    if (showNotificationsInfo) {
        val alreadyAllowed = Permissions.hasNotifications(context)
        fun close(askPermission: Boolean) {
            prefs.notificationsInfoShown = true
            showNotificationsInfo = false
            if (askPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !alreadyAllowed) {
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            viewModel.scan()
        }
        AlertDialog(
            onDismissRequest = { close(askPermission = false) },
            title = { Text("Ative as notificações") },
            text = {
                Text(
                    "O MestreScan avisa quando um app suspeito aparecer e também manda ofertas da " +
                        "${stringResource(R.string.brand_store)}. Se quiser, você pode silenciar só as " +
                        "ofertas depois, na tela de Ajuda."
                )
            },
            confirmButton = {
                TextButton(onClick = { close(askPermission = true) }) {
                    Text(if (alreadyAllowed) "Entendi" else "Ativar")
                }
            },
            dismissButton = if (alreadyAllowed) null else {
                { TextButton(onClick = { close(askPermission = false) }) { Text("Agora não") } }
            },
        )
    }

    ScreenFrame(title = null, onBack = null) {
        item { BrandHeader() }
        item { ScanButton(scanning = state.scanning, onClick = ::startScan) }
        item {
            val last = state.lastScan
            Text(
                text = when {
                    last == null -> "Toque em Escanear para analisar os apps do celular."
                    last.danger + last.suspect == 0 -> "Último scan: ${formatDateTime(last.at)} — nenhum suspeito ✅"
                    else -> "Último scan: ${formatDateTime(last.at)} — " +
                        "${plural(last.danger, "perigoso", "perigosos")}, ${plural(last.suspect, "suspeito", "suspeitos")}"
                },
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = if (last != null && last.danger > 0) RiskColors.danger else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (!state.hasUsageAccess) {
            item {
                SectionCard(borderColor = MaterialTheme.colorScheme.primary) {
                    Text("Ative a detecção ao ligar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "Vírus de propaganda costuma começar uns 30 segundos depois de ligar o celular. " +
                            "Com o \"acesso ao uso\" liberado, o MestreScan descobre qual app abriu a propaganda.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "Na próxima tela, encontre MestreScan na lista e ative.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    OutlinedButton(onClick = { SystemActions.openUsageAccessSettings(context) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Ativar agora")
                    }
                }
            }
        }
        item {
            MenuItem(Icons.Filled.PowerSettingsNew, "O que abriu ao ligar", "Descubra quem mostra propaganda depois de ligar") {
                viewModel.open(Screen.BootTimeline)
                viewModel.refreshBoot()
            }
        }
        item {
            MenuItem(Icons.Filled.VisibilityOff, "Apps escondidos", "Apps instalados sem ícone na tela") {
                if (state.results == null) viewModel.scan(silent = true)
                viewModel.open(Screen.Hidden)
            }
        }
        item {
            MenuItem(Icons.AutoMirrored.Filled.HelpOutline, "Celular travado? Ajuda", "Modo seguro passo a passo") {
                viewModel.open(Screen.Help)
            }
        }
        item { WhatsAppCta() }
        item { StoreFooter() }
    }
}

@Composable
private fun BrandHeader() {
    Column(
        Modifier.fillMaxWidth().padding(top = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(R.drawable.brand_logo),
            contentDescription = stringResource(R.string.brand_store),
            modifier = Modifier.size(150.dp).clip(CircleShape),
        )
        Spacer(Modifier.height(4.dp))
        val name = stringResource(R.string.app_name)
        Text(
            // "Mestre" branco + "Scan" dourado, no estilo do logo MestreCell.
            text = buildAnnotatedString {
                val split = name.indexOf("Scan").takeIf { it > 0 } ?: name.length
                append(name.substring(0, split))
                withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) { append(name.substring(split)) }
            },
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            stringResource(R.string.brand_slogan),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ScanButton(scanning: Boolean, onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
        Button(
            onClick = onClick,
            enabled = !scanning,
            shape = CircleShape,
            modifier = Modifier.size(190.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            ),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (scanning) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("Analisando…", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(56.dp))
                    Text("ESCANEAR", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
private fun MenuItem(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    SectionCard(modifier = Modifier.clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.width(4.dp))
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        }
    }
}
