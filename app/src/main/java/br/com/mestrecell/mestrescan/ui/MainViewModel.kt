package br.com.mestrecell.mestrescan.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.com.mestrecell.mestrescan.boot.BootHistory
import br.com.mestrecell.mestrescan.boot.BootObserver
import br.com.mestrecell.mestrescan.boot.BootRecord
import br.com.mestrecell.mestrescan.data.AppPrefs
import br.com.mestrecell.mestrescan.data.DeviceInspector
import br.com.mestrecell.mestrescan.data.RiskEngine
import br.com.mestrecell.mestrescan.data.RiskLevel
import br.com.mestrecell.mestrescan.data.RuleSet
import br.com.mestrecell.mestrescan.data.ScanResult
import br.com.mestrecell.mestrescan.system.Permissions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface Screen {
    data object Home : Screen
    data object Results : Screen
    data class Detail(val packageName: String) : Screen
    data object Hidden : Screen
    data object BootTimeline : Screen
    data object Help : Screen
}

data class UiState(
    val stack: List<Screen> = listOf(Screen.Home),
    val scanning: Boolean = false,
    /** null = ainda não escaneou nesta sessão. */
    val results: List<ScanResult>? = null,
    val lastScan: AppPrefs.LastScan? = null,
    val boots: List<BootRecord> = emptyList(),
    val hasUsageAccess: Boolean = false,
    val trusted: Set<String> = emptySet(),
) {
    val screen: Screen get() = stack.last()

    val suspects: List<ScanResult>
        get() = results.orEmpty()
            .filter { it.level != RiskLevel.SAFE && it.app.packageName !in trusted }
            .sortedByDescending { it.score }

    fun resultFor(packageName: String): ScanResult? =
        results?.firstOrNull { it.app.packageName == packageName }
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val rules = RuleSet.load(application)
    private val inspector = DeviceInspector(application)
    private val engine = RiskEngine(rules)
    private val observer = BootObserver(application, rules)
    private val history = BootHistory(application)
    private val prefs = AppPrefs(application)
    private var scanJob: Job? = null

    private val _state = MutableStateFlow(
        UiState(lastScan = prefs.lastScan, trusted = prefs.trusted, boots = history.load())
    )
    val state = _state.asStateFlow()

    val bootWindowSeconds: Int get() = rules.bootWindowSeconds
    fun currentBootTime(): Long = observer.currentBootTime()

    init {
        refreshPermissions()
    }

    fun open(screen: Screen) = _state.update { it.copy(stack = it.stack + screen) }

    fun back(): Boolean {
        if (_state.value.stack.size <= 1) return false
        _state.update { it.copy(stack = it.stack.dropLast(1)) }
        return true
    }

    fun openBootTimeline() {
        _state.update { it.copy(stack = listOf(Screen.Home, Screen.BootTimeline)) }
        refreshBoot()
    }

    /** Voltou de outra tela (configurações, desinstalar, WhatsApp): atualiza sem travar a tela. */
    fun onResume() {
        refreshPermissions()
        if (_state.value.results != null) scan(silent = true)
    }

    fun refreshPermissions() {
        _state.update { it.copy(hasUsageAccess = Permissions.hasUsageAccess(getApplication())) }
    }

    fun scan(silent: Boolean = false) {
        if (scanJob?.isActive == true) return
        scanJob = viewModelScope.launch {
            if (!silent) _state.update { it.copy(scanning = true) }
            val started = System.currentTimeMillis()
            val (results, boots) = withContext(Dispatchers.Default) {
                val boots = if (Permissions.hasUsageAccess(getApplication())) history.refresh(observer) else history.load()
                val evidence = BootObserver.evidence(boots)
                inspector.userApps().map { engine.evaluate(it, evidence[it.packageName]) } to boots
            }
            // Um instante de "analisando" para o usuário perceber que o scan rodou.
            if (!silent) delay((MIN_SCAN_MS - (System.currentTimeMillis() - started)).coerceAtLeast(0))

            val trusted = _state.value.trusted
            val visible = results.filter { it.app.packageName !in trusted }
            val last = AppPrefs.LastScan(
                at = System.currentTimeMillis(),
                danger = visible.count { it.level == RiskLevel.DANGER },
                suspect = visible.count { it.level == RiskLevel.SUSPECT },
            )
            prefs.saveLastScan(last)
            _state.update {
                it.copy(
                    scanning = false,
                    results = results,
                    boots = boots,
                    lastScan = last,
                    stack = if (!silent && it.screen == Screen.Home) it.stack + Screen.Results else it.stack,
                )
            }
        }
    }

    fun refreshBoot() {
        viewModelScope.launch {
            if (!Permissions.hasUsageAccess(getApplication())) return@launch
            val boots = withContext(Dispatchers.Default) { history.refresh(observer) }
            _state.update { it.copy(boots = boots) }
        }
    }

    fun trust(packageName: String) = setTrusted(_state.value.trusted + packageName)

    fun untrust(packageName: String) = setTrusted(_state.value.trusted - packageName)

    private fun setTrusted(trusted: Set<String>) {
        prefs.trusted = trusted
        _state.update { it.copy(trusted = trusted) }
    }

    /**
     * Confere se o usuário confirmou a desinstalação. O Android pode levar
     * um instante para terminar, então tentamos por ~2 s.
     */
    suspend fun confirmRemoved(packageName: String): Boolean {
        repeat(6) {
            if (!inspector.isInstalled(packageName)) {
                _state.update { s ->
                    s.copy(
                        results = s.results?.filterNot { it.app.packageName == packageName },
                        stack = s.stack.filterNot { it == Screen.Detail(packageName) },
                    )
                }
                return true
            }
            delay(400)
        }
        return false
    }

    private companion object {
        const val MIN_SCAN_MS = 1200L
    }
}
