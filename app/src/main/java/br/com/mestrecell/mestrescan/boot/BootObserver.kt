package br.com.mestrecell.mestrescan.boot

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import br.com.mestrecell.mestrescan.data.DeviceInspector
import br.com.mestrecell.mestrescan.data.RuleSet
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.min

/** Um app (instalado pelo usuário) que apareceu na tela logo depois de ligar. */
data class BootLaunch(
    val packageName: String,
    val label: String,
    val secondsAfterBoot: Int,
    val className: String?,
    val adScreen: Boolean,
    val beforeUnlock: Boolean,
    val hasIcon: Boolean,
    /** Abriu sem o usuário tocar no ícone (pela nossa melhor estimativa). */
    val selfStarted: Boolean,
)

/** [bootCount] identifica o reinício com certeza; é null nos achados só pelo histórico. */
data class BootRecord(val bootTime: Long, val launches: List<BootLaunch>, val bootCount: Int? = null)

/** Resumo, por app, de todos os reinícios observados. */
data class BootEvidence(
    val bootsWithLaunch: Int,
    val totalBoots: Int,
    val firstSeconds: Int,
    val adScreen: Boolean,
)

/**
 * Lê o histórico de uso que o próprio Android guarda (alguns dias) e descobre
 * quem abriu tela nos primeiros minutos depois de ligar. Funciona mesmo que o
 * MestreScan não estivesse rodando naquele momento.
 */
class BootObserver(private val context: Context, private val rules: RuleSet) {
    private val usageStats = context.getSystemService(UsageStatsManager::class.java)
    private val inspector = DeviceInspector(context)

    fun currentBootTime(): Long = System.currentTimeMillis() - SystemClock.elapsedRealtime()

    /** Contador de boots do Android: único por reinício. */
    fun currentBootCount(): Int? =
        Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, -1).takeIf { it >= 0 }

    /**
     * Reinícios anteriores ao atual que ainda estão no histórico (Android 10+).
     * O evento de "ligou" é gravado alguns segundos depois do boot real, então a
     * janela começa um pouco antes dele (sem passar do desligamento anterior).
     */
    fun pastBootTimes(): List<Long> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return emptyList()
        val current = currentBootTime()
        val now = System.currentTimeMillis()
        val events = usageStats.queryEvents(now - TimeUnit.DAYS.toMillis(HISTORY_DAYS), now)
        val event = UsageEvents.Event()
        val times = mutableListOf<Long>()
        var lastShutdown = 0L
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            when (event.eventType) {
                UsageEvents.Event.DEVICE_SHUTDOWN -> lastShutdown = event.timeStamp
                UsageEvents.Event.DEVICE_STARTUP ->
                    // Eventos depois do boot atual são do próprio boot atual.
                    if (event.timeStamp < current - CLOCK_SLACK_MS) {
                        times += maxOf(lastShutdown, event.timeStamp - STARTUP_DELAY_MS)
                    }
            }
        }
        return times.distinct()
    }

    fun analyze(bootTime: Long): BootRecord {
        val end = min(bootTime + rules.bootWindowSeconds * 1000L, System.currentTimeMillis())
        val homes = inspector.homePackages()
        val launchers = inspector.launcherPackages()
        val labels = HashMap<String, String?>()

        val events = usageStats.queryEvents(bootTime, end)
        val event = UsageEvents.Event()
        // Antes do Android 9 não há evento de desbloqueio; assumimos desbloqueado.
        var unlocked = Build.VERSION.SDK_INT < Build.VERSION_CODES.P
        var previous: String? = null
        val found = LinkedHashMap<String, BootLaunch>()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            when (event.eventType) {
                UsageEvents.Event.KEYGUARD_HIDDEN -> unlocked = true
                UsageEvents.Event.KEYGUARD_SHOWN -> unlocked = false
                @Suppress("DEPRECATION")
                UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    val pkg = event.packageName
                    val label = labels.getOrPut(pkg) { inspector.userAppLabel(pkg) }
                    if (pkg != context.packageName && label != null) {
                        val adScreen = rules.isAdScreen(event.className)
                        val existing = found[pkg]
                        if (existing == null) {
                            val hasIcon = pkg in launchers
                            // Só consideramos "você abriu" se veio da tela inicial, tem ícone,
                            // o celular já estava desbloqueado e não é tela de anúncio.
                            val selfStarted = !hasIcon || adScreen || !unlocked || previous !in homes
                            found[pkg] = BootLaunch(
                                packageName = pkg,
                                label = label,
                                secondsAfterBoot = ((event.timeStamp - bootTime) / 1000).toInt().coerceAtLeast(0),
                                className = event.className,
                                adScreen = adScreen,
                                beforeUnlock = !unlocked,
                                hasIcon = hasIcon,
                                selfStarted = selfStarted,
                            )
                        } else if (adScreen && !existing.adScreen) {
                            found[pkg] = existing.copy(adScreen = true, className = event.className, selfStarted = true)
                        }
                    }
                    previous = pkg
                }
            }
        }
        return BootRecord(bootTime, found.values.toList())
    }

    companion object {
        const val HISTORY_DAYS = 7L
        private const val STARTUP_DELAY_MS = 90_000L
        private const val CLOCK_SLACK_MS = 10_000L

        /** Para registros sem contador: horários tão próximos são o mesmo reinício. */
        fun closeInTime(a: Long, b: Long): Boolean = abs(a - b) < 150_000L

        fun evidence(records: List<BootRecord>): Map<String, BootEvidence> {
            val selfLaunches = records.flatMap { r -> r.launches.filter { it.selfStarted } }
            return selfLaunches.groupBy { it.packageName }.mapValues { (_, launches) ->
                BootEvidence(
                    bootsWithLaunch = launches.size,
                    totalBoots = records.size,
                    firstSeconds = launches.minOf { it.secondsAfterBoot },
                    adScreen = launches.any { it.adScreen },
                )
            }
        }
    }
}
