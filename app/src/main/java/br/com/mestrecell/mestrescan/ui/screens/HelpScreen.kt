package br.com.mestrecell.mestrescan.ui.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import br.com.mestrecell.mestrescan.system.SystemActions
import br.com.mestrecell.mestrescan.ui.MainViewModel
import br.com.mestrecell.mestrescan.ui.ScreenFrame
import br.com.mestrecell.mestrescan.ui.SectionCard
import br.com.mestrecell.mestrescan.ui.StoreFooter
import br.com.mestrecell.mestrescan.ui.WhatsAppCta

@Composable
fun HelpScreen(viewModel: MainViewModel) {
    ScreenFrame(title = "Ajuda", onBack = { viewModel.back() }) {
        item {
            SectionCard {
                Text("Celular travado de propaganda? Use o modo seguro", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("No modo seguro os apps baixados ficam desligados, então a propaganda para e você consegue mexer.")
                Step(1, "Segure o botão de ligar até aparecer \"Desligar\".")
                Step(2, "Toque e segure em \"Desligar\" até aparecer \"Modo seguro\". Toque em OK.")
                Muted("Samsung e outros: desligue, ligue e segure o volume para baixo quando aparecer o logo.")
                Step(3, "Com o celular em modo seguro, vá em Configurações › Apps.")
                Step(4, "Desinstale o app que o MestreScan apontou.")
                Muted(
                    "Se o botão Desinstalar estiver apagado: Configurações › Segurança › Administradores do dispositivo, " +
                        "desative o app e tente de novo."
                )
                Step(5, "Reinicie o celular normalmente e rode o MestreScan de novo para confirmar.")
                Muted("Atenção: no modo seguro o MestreScan também fica desligado. Anote o nome do app antes.")
            }
        }
        item {
            SectionCard {
                Text("Como evitar que volte", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("• Instale apps só pela Play Store.")
                Text("• Mantenha o Play Protect ligado (Play Store › seu perfil › Play Protect).")
                Text("• Desconfie de apps \"limpadores\", \"aceleradores\" e \"VPN grátis\".")
                Text("• Não toque em anúncios que dizem que seu celular está com vírus.")
            }
        }
        item {
            val context = LocalContext.current
            SectionCard {
                Text("Notificações", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Os alertas de segurança e as ofertas da loja chegam em canais separados. " +
                        "Você pode silenciar só as ofertas e continuar recebendo os alertas.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                    onClick = { SystemActions.openOffersChannelSettings(context) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Silenciar ofertas") }
            }
        }
        item { WhatsAppCta() }
        item { StoreFooter() }
    }
}

@Composable
private fun Step(number: Int, text: String) {
    Text("$number. $text")
}

@Composable
private fun Muted(text: String) {
    Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
}
