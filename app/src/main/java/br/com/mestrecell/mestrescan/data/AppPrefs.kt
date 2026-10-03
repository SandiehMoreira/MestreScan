package br.com.mestrecell.mestrescan.data

import android.content.Context

/** Apps que o usuário marcou como confiáveis e o resumo do último scan. */
class AppPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("mestrescan", Context.MODE_PRIVATE)

    var trusted: Set<String>
        get() = prefs.getStringSet(KEY_TRUSTED, emptySet()).orEmpty().toSet()
        set(value) = prefs.edit().putStringSet(KEY_TRUSTED, value).apply()

    /** Já mostramos o aviso de notificações (alertas + ofertas da loja). */
    var notificationsInfoShown: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATIONS_INFO, false)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFICATIONS_INFO, value).apply()

    val lastScan: LastScan?
        get() {
            val at = prefs.getLong(KEY_SCAN_AT, 0L)
            if (at == 0L) return null
            return LastScan(at, prefs.getInt(KEY_SCAN_DANGER, 0), prefs.getInt(KEY_SCAN_SUSPECT, 0))
        }

    fun saveLastScan(scan: LastScan) {
        prefs.edit()
            .putLong(KEY_SCAN_AT, scan.at)
            .putInt(KEY_SCAN_DANGER, scan.danger)
            .putInt(KEY_SCAN_SUSPECT, scan.suspect)
            .apply()
    }

    data class LastScan(val at: Long, val danger: Int, val suspect: Int)

    private companion object {
        const val KEY_TRUSTED = "trusted"
        const val KEY_NOTIFICATIONS_INFO = "notifications_info_shown"
        const val KEY_SCAN_AT = "last_scan_at"
        const val KEY_SCAN_DANGER = "last_scan_danger"
        const val KEY_SCAN_SUSPECT = "last_scan_suspect"
    }
}
