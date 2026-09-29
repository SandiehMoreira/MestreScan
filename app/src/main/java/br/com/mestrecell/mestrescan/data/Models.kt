package br.com.mestrecell.mestrescan.data

import android.content.ComponentName

enum class RiskLevel { SAFE, SUSPECT, DANGER }

enum class OverlayState { NONE, REQUESTED, GRANTED }

/** O que conseguimos saber de um app instalado sem ADB. */
data class InstalledApp(
    val packageName: String,
    val label: String,
    val installedAt: Long,
    val installer: String?,
    val hasLauncherIcon: Boolean,
    val adminComponents: List<ComponentName>,
    val hasAccessibility: Boolean,
    val overlay: OverlayState,
    val canInstallApps: Boolean,
    val startsOnBoot: Boolean,
) {
    val isDeviceAdmin: Boolean get() = adminComponents.isNotEmpty()
}

data class Reason(val points: Int, val text: String)

data class ScanResult(
    val app: InstalledApp,
    val score: Int,
    val level: RiskLevel,
    val reasons: List<Reason>,
)
