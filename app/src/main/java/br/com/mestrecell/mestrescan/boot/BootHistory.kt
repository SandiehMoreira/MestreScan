package br.com.mestrecell.mestrescan.boot

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Guarda nossa própria cópia de cada reinício analisado, porque o Android
 * apaga o histórico de uso depois de alguns dias.
 */
class BootHistory(context: Context) {
    private val file = File(context.filesDir, "boot_history.json")

    fun load(): List<BootRecord> = try {
        if (!file.exists()) emptyList() else fromJson(file.readText())
    } catch (e: Exception) {
        emptyList()
    }

    /**
     * Analisa o reinício atual, junta com os salvos e com os anteriores que ainda
     * estão no histórico do Android, e grava. O primeiro da lista é sempre o atual.
     */
    @Synchronized
    fun refresh(observer: BootObserver): List<BootRecord> {
        val count = observer.currentBootCount()
        val current = observer.analyze(observer.currentBootTime()).copy(bootCount = count)
        // O histórico do Android ainda tem tudo do boot atual, então a nova análise substitui a salva.
        val stored = load().filterNot {
            if (count != null) it.bootCount == count
            else BootObserver.closeInTime(it.bootTime, current.bootTime)
        }
        // Reinícios de antes de o app estar rodando: só os que ainda não temos salvos.
        val past = observer.pastBootTimes()
            .filter { t -> stored.none { BootObserver.closeInTime(it.bootTime, t) } }
            .map { observer.analyze(it) }
        val result = (listOf(current) + (stored + past).sortedByDescending { it.bootTime })
            .take(MAX_RECORDS)
        file.writeText(toJson(result))
        return result
    }

    private fun toJson(records: List<BootRecord>): String = JSONArray().apply {
        records.forEach { record ->
            put(JSONObject().apply {
                put("bootTime", record.bootTime)
                put("bootCount", record.bootCount ?: JSONObject.NULL)
                put("launches", JSONArray().apply {
                    record.launches.forEach { l ->
                        put(JSONObject().apply {
                            put("packageName", l.packageName)
                            put("label", l.label)
                            put("secondsAfterBoot", l.secondsAfterBoot)
                            put("className", l.className ?: JSONObject.NULL)
                            put("adScreen", l.adScreen)
                            put("beforeUnlock", l.beforeUnlock)
                            put("hasIcon", l.hasIcon)
                            put("selfStarted", l.selfStarted)
                        })
                    }
                })
            })
        }
    }.toString()

    private fun fromJson(json: String): List<BootRecord> {
        val array = JSONArray(json)
        return List(array.length()) { i ->
            val o = array.getJSONObject(i)
            val launches = o.getJSONArray("launches")
            BootRecord(
                bootTime = o.getLong("bootTime"),
                bootCount = if (o.isNull("bootCount")) null else o.getInt("bootCount"),
                launches = List(launches.length()) { j ->
                    val l = launches.getJSONObject(j)
                    BootLaunch(
                        packageName = l.getString("packageName"),
                        label = l.getString("label"),
                        secondsAfterBoot = l.getInt("secondsAfterBoot"),
                        className = if (l.isNull("className")) null else l.getString("className"),
                        adScreen = l.getBoolean("adScreen"),
                        beforeUnlock = l.getBoolean("beforeUnlock"),
                        hasIcon = l.getBoolean("hasIcon"),
                        selfStarted = l.getBoolean("selfStarted"),
                    )
                },
            )
        }
    }

    private companion object {
        const val MAX_RECORDS = 20
    }
}
