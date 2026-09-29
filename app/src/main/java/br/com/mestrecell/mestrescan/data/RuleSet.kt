package br.com.mestrecell.mestrescan.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Tabela de pontos e listas, em formato de dados (assets/rules.json).
 * É o mesmo formato que o backend do Projeto 1 vai servir, para os dois
 * projetos pontuarem igual sem duplicar código.
 */
class RuleSet(
    val yellowFrom: Int,
    val redFrom: Int,
    private val points: Map<String, Int>,
    val recentInstallDays: Int,
    val bootWindowSeconds: Int,
    val genericNameKeywords: List<String>,
    val imitatesSystemKeywords: List<String>,
    val trustedInstallers: Set<String>,
    private val whitelist: List<String>,
    val knownMalicious: Set<String>,
    private val adScreenPatterns: List<String>,
) {
    fun pointsFor(ruleId: String): Int = points[ruleId] ?: 0

    /** Entradas terminadas em "." valem como prefixo (ex.: "com.google."). */
    fun isWhitelisted(packageName: String): Boolean =
        whitelist.any { if (it.endsWith(".")) packageName.startsWith(it) else packageName == it }

    fun isAdScreen(className: String?): Boolean =
        className != null && adScreenPatterns.any { className.contains(it, ignoreCase = true) }

    fun levelFor(score: Int): RiskLevel = when {
        score >= redFrom -> RiskLevel.DANGER
        score >= yellowFrom -> RiskLevel.SUSPECT
        else -> RiskLevel.SAFE
    }

    companion object {
        @Volatile
        private var cached: RuleSet? = null

        fun load(context: Context): RuleSet = cached ?: synchronized(this) {
            cached ?: parse(
                context.assets.open("rules.json").bufferedReader().use { it.readText() }
            ).also { cached = it }
        }

        fun parse(json: String): RuleSet {
            val root = JSONObject(json)
            val bands = root.getJSONObject("bands")
            val pointsJson = root.getJSONObject("points")
            val points = pointsJson.keys().asSequence().associateWith { pointsJson.getInt(it) }
            return RuleSet(
                yellowFrom = bands.getInt("yellow"),
                redFrom = bands.getInt("red"),
                points = points,
                recentInstallDays = root.getInt("recentInstallDays"),
                bootWindowSeconds = root.getInt("bootWindowSeconds"),
                genericNameKeywords = root.getJSONArray("genericNameKeywords").strings(),
                imitatesSystemKeywords = root.getJSONArray("imitatesSystemKeywords").strings(),
                trustedInstallers = root.getJSONArray("trustedInstallers").strings().toSet(),
                whitelist = root.getJSONArray("whitelist").strings(),
                knownMalicious = root.getJSONArray("knownMalicious").strings().toSet(),
                adScreenPatterns = root.getJSONArray("adScreenPatterns").strings(),
            )
        }

        private fun JSONArray.strings(): List<String> = List(length()) { getString(it) }
    }
}
