package br.com.mestrecell.mestrescan.data

import br.com.mestrecell.mestrescan.boot.BootEvidence
import java.util.concurrent.TimeUnit

/** Soma os pontos de cada sinal e explica o motivo em linguagem simples. */
class RiskEngine(private val rules: RuleSet) {

    fun evaluate(app: InstalledApp, boot: BootEvidence?, now: Long = System.currentTimeMillis()): ScanResult {
        val reasons = mutableListOf<Reason>()
        fun add(ruleId: String, text: String) {
            val points = rules.pointsFor(ruleId)
            if (points > 0) reasons += Reason(points, text)
        }

        if (app.packageName in rules.knownMalicious) {
            add("known_malicious", "Está na lista de apps maliciosos conhecidos")
        }

        val fromTrustedStore = app.installer in rules.trustedInstallers
        // Lista branca só vale se veio de loja confiável: um vírus pode usar
        // um nome de pacote parecido com "com.google.".
        if (reasons.isEmpty() && fromTrustedStore && rules.isWhitelisted(app.packageName)) {
            return ScanResult(app, 0, RiskLevel.SAFE, emptyList())
        }

        if (app.isDeviceAdmin) add("device_admin", "É administrador do celular (dificulta a remoção)")
        if (app.hasAccessibility) add("accessibility", "Tem acesso de acessibilidade (consegue controlar a tela)")
        when (app.overlay) {
            OverlayState.GRANTED -> add("overlay_granted", "Pode aparecer por cima de outros apps")
            OverlayState.REQUESTED -> add("overlay_requested", "Pede para aparecer por cima de outros apps")
            OverlayState.NONE -> Unit
        }
        if (!app.hasLauncherIcon) add("hidden", "Está escondido: não tem ícone na tela")
        if (!fromTrustedStore) add("sideload", "Instalado fora da Play Store")
        if (app.canInstallApps) add("install_packages", "Pode instalar outros apps")

        val name = app.label.lowercase()
        val pkg = app.packageName.lowercase()
        if (rules.genericNameKeywords.any { name.contains(it) || pkg.contains(it.replace(" ", "")) }) {
            add("generic_name", "Nome típico de app falso (limpador, acelerador…)")
        }
        if (rules.imitatesSystemKeywords.any { name.contains(it) }) {
            add("imitates_system", "Nome imita app do sistema, mas não é do sistema")
        }
        if (app.startsOnBoot) add("boot_start", "Inicia sozinho quando o celular liga")

        val days = TimeUnit.MILLISECONDS.toDays(now - app.installedAt)
        if (days <= rules.recentInstallDays) {
            add("recent_install", if (days == 0L) "Instalado hoje" else "Instalado há $days dia(s)")
        }

        if (boot != null && boot.bootsWithLaunch > 0) {
            add("boot_popup", "Abriu sozinho ${boot.firstSeconds} s depois de ligar o celular")
            if (boot.adScreen) add("boot_ad_screen", "Abriu uma tela de propaganda")
            if (boot.bootsWithLaunch >= 2) {
                add("boot_repeat", "Repetiu em ${boot.bootsWithLaunch} de ${boot.totalBoots} vezes que o celular ligou")
            }
        }

        val score = reasons.sumOf { it.points }
        return ScanResult(app, score, rules.levelFor(score), reasons.sortedByDescending { it.points })
    }
}
